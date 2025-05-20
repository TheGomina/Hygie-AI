import csv
import pytest
from pathlib import Path

from backend.bmp_service.anticholinergic import _load_scores, compute_burden_score, _resources_dir

@ pytest.fixture(autouse=True)
def fake_resources_dir(monkeypatch, tmp_path):
    # Create fake CSV files in temp resources dir
    res_dir = tmp_path / "resources"
    res_dir.mkdir()
    # file1: valid entries and header
    f1 = res_dir / "test-CIA-ACB-1.csv"
    rows = [
        [';comment'],
        ['drug1', '5', '', ''],
        ['drug2', '', '', '2'],
        ['drug1', '3', '', ''],
        ['malformed', 'X', '', '']
    ]
    with f1.open('w', encoding='utf-8', newline='') as fh:
        writer = csv.writer(fh, delimiter=';')
        for r in rows:
            writer.writerow(r)
    # file2: another file to test max accumulation
    f2 = res_dir / "CIA-ACB-extra.csv"
    with f2.open('w', encoding='utf-8', newline='') as fh:
        writer = csv.writer(fh, delimiter=';')
        writer.writerow(['drug2', '1', '', '3'])
    # Monkeypatch resources dir
    monkeypatch.setattr('backend.bmp_service.anticholinergic._resources_dir', lambda: res_dir)
    # Clear cache
    _load_scores.cache_clear()
    yield


def test_load_scores_mapping():
    mapping = _load_scores()
    # drug1: max(5,3) = 5
    assert mapping['DRUG1'] == 5
    # drug2: max(2,3,1?) = 3
    assert mapping['DRUG2'] == 3
    # malformed entry skipped (score 0)
    assert 'MALFORMED' not in mapping


def test_compute_burden_score():
    # Using mapping from test_load_scores_mapping
    score = compute_burden_score(['drug1', 'drug2', 'unknown'])
    assert score == 5 + 3 + 0
    # Case insensitive
    assert compute_burden_score(['DRUG1', 'Drug2']) == score
