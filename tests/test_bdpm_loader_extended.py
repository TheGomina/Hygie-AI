import sqlite3
import csv
import pytest
from pathlib import Path

from backend.bmp_service.bdpm_loader import _parse_csv, _parse_sqlite, get_mapping, normalize_drug, _FALLBACK


def test_parse_csv(tmp_path):
    # create CSV file
    csv_file = tmp_path / "bdpm_substances.csv"
    fieldnames = ["CIP7", "CIP13", "DCI"]
    with csv_file.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames, delimiter=";")
        writer.writeheader()
        writer.writerow({"CIP7": "1234567", "CIP13": "", "DCI": "sub1"})
        writer.writerow({"CIP7": "", "CIP13": "7654321", "DCI": "sub2"})
    mapping = _parse_csv(csv_file)
    assert mapping["1234567"] == "SUB1"
    assert mapping["7654321"] == "SUB2"


def test_parse_sqlite(tmp_path):
    db_file = tmp_path / "bdpm.sqlite"
    conn = sqlite3.connect(str(db_file))
    cur = conn.cursor()
    cur.execute("CREATE TABLE compositions (cis TEXT, substance TEXT)")
    cur.execute("CREATE TABLE cips (cip TEXT, cis TEXT)")
    cur.execute("INSERT INTO compositions (cis, substance) VALUES (?, ?)", ("001", "DRUGA"))
    cur.execute("INSERT INTO compositions (cis, substance) VALUES (?, ?)", ("002", "DRUGB"))
    cur.execute("INSERT INTO cips (cip, cis) VALUES (?, ?)", ("123", "001"))
    conn.commit()
    conn.close()
    mapping = _parse_sqlite(db_file)
    assert mapping["001"] == "DRUGA"
    assert mapping["DRUGB"] == "DRUGB"
    assert mapping["123"] == "DRUGA"


@pytest.fixture(autouse=True)
def reset_paths(monkeypatch, tmp_path):
    # override both DB and CSV paths to nonexistent by default
    from backend.bmp_service import bdpm_loader
    monkeypatch.setattr(bdpm_loader, 'DB_PATH', tmp_path / 'no.db')
    monkeypatch.setattr(bdpm_loader, 'CSV_PATH', tmp_path / 'no.csv')
    try:
        bdpm_loader.get_mapping.cache_clear()
    except AttributeError:
        pass
    yield
    try:
        bdpm_loader.get_mapping.cache_clear()
    except AttributeError:
        pass


def test_get_mapping_fallback():
    mapping = get_mapping()
    assert mapping == _FALLBACK


def test_get_mapping_csv(monkeypatch, tmp_path):
    # simulate CSV only
    from backend.bmp_service import bdpm_loader
    csv_file = tmp_path / "bdpm_substances.csv"
    with csv_file.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["CIP7", "CIP13", "DCI"], delimiter=";")
        writer.writeheader()
        writer.writerow({"CIP7": "7777777", "CIP13": "", "DCI": "drugx"})
    monkeypatch.setattr(bdpm_loader, 'CSV_PATH', csv_file)
    mapping = get_mapping()
    assert mapping["7777777"] == "DRUGX"


def test_get_mapping_sqlite(monkeypatch, tmp_path):
    # simulate SQLite priority
    from backend.bmp_service import bdpm_loader
    db_file = tmp_path / "bdpm.sqlite"
    conn = sqlite3.connect(str(db_file))
    cur = conn.cursor()
    cur.execute("CREATE TABLE compositions (cis TEXT, substance TEXT)")
    cur.execute("CREATE TABLE cips (cip TEXT, cis TEXT)")
    cur.execute("INSERT INTO compositions(cis, substance) VALUES (?, ?)", ("C1", "abc"))
    conn.commit()
    conn.close()
    monkeypatch.setattr(bdpm_loader, 'DB_PATH', db_file)
    mapping = get_mapping()
    assert mapping["C1"] == "abc"


def test_normalize_drug(monkeypatch):
    fake_map = {"A": "AA", "B": "BB"}
    monkeypatch.setattr('backend.bmp_service.bdpm_loader.get_mapping', lambda: fake_map)
    assert normalize_drug("a") == "AA"
    assert normalize_drug("B") == "BB"
    assert normalize_drug("C") == "C"
