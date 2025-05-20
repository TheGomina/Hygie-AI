import csv
import pytest
from pathlib import Path

from backend.bmp_service.anticholinergic import _load_scores


def test_load_scores_acb_error(tmp_path, monkeypatch):
    # Test ValueError on ACB column fallback
    f = tmp_path / 'CIA-ACB-test.csv'
    with f.open('w', encoding='utf-8', newline='') as fh:
        writer = csv.writer(fh, delimiter=';')
        # cia=2, acb malformed
        writer.writerow(['drug3', '2', '', 'X'])
    # point resources dir to tmp
    monkeypatch.setattr('backend.bmp_service.anticholinergic._resources_dir', lambda: tmp_path)
    _load_scores.cache_clear()
    mapping = _load_scores()
    assert mapping.get('DRUG3') == 2


def test_load_scores_file_error(monkeypatch):
    # Simulate file open error in resources dir
    class DummyDir:
        def glob(self, pattern):
            return [self]
        def open(self, *args, **kwargs):
            raise IOError('fail')
    monkeypatch.setattr('backend.bmp_service.anticholinergic._resources_dir', lambda: DummyDir())
    _load_scores.cache_clear()
    mapping = _load_scores()
    assert mapping == {}
