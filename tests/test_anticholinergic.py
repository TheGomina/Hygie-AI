import pytest
from pathlib import Path
from backend.bmp_service import anticholinergic as ac


def setup_function():
    # Clear cache before each test
    ac._load_scores.cache_clear()


def test_load_scores_empty(tmp_path, monkeypatch):
    # No CSV files → empty mapping
    monkeypatch.setattr(ac, '_resources_dir', lambda: tmp_path)
    mapping = ac._load_scores()
    assert mapping == {}


def test_load_scores_and_compute(tmp_path, monkeypatch):
    # Prepare fake resources
    monkeypatch.setattr(ac, '_resources_dir', lambda: tmp_path)
    # Good CSV
    good = tmp_path / 'CIA-ACB.csv'
    good.write_text(
        'ASPIRIN;2;;\n'
        'IBUPROFEN;; ;4\n'
        'ASPIRIN;1;;5\n', encoding='utf-8'
    )
    # Malformed CSV should be ignored
    bad = tmp_path / 'CIA-ACB-bad.csv'
    bad.write_text('BROKEN_LINE', encoding='utf-8')

    # Load mapping
    mapping = ac._load_scores()
    assert mapping == {'ASPIRIN': 5, 'IBUPROFEN': 4}

    # Compute burden score
    score = ac.compute_burden_score(['aspirin', 'ibuprofen', 'unknown'])
    assert score == 5 + 4 + 0
    ac._load_scores.cache_clear()

def test_score_parsing_variants(tmp_path, monkeypatch):
    # Header row and blank rows should be skipped; test ValueError branches
    monkeypatch.setattr(ac, '_resources_dir', lambda: tmp_path)
    csv_file = tmp_path / 'CIA-ACB.csv'
    csv_file.write_text(
        ';HEADER;X;Y\n'
        'ASP;2;;\n'
        'ASP;1;;5\n'
        'BAD;bad;;10\n'
        'ERR;3;;bad\n'
        'NONE;;; \n', encoding='utf-8'
    )
    mapping = ac._load_scores()
    # ASP max(2,5)=5; BAD: score_cia fails->0, score_acb=10; ERR: score_cia=3, score_acb fails->0
    assert mapping == {'ASP': 5, 'BAD': 10, 'ERR': 3}
    score = ac.compute_burden_score(['asp', 'bad', 'err', 'none'])
    assert score == 5 + 10 + 3 + 0
