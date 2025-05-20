import csv
import sqlite3
import pytest
from pathlib import Path
from backend.bmp_service import bdpm_loader as loader


def test_parse_csv(tmp_path):
    csv_file = tmp_path / "test.csv"
    # Prepare CSV with header and rows
    rows = [
        {"CIP7": "1234567", "CIP13": "", "DCI": "test1"},
        {"CIP7": "", "CIP13": "7654321", "DCI": "test2"},
        {"CIP7": "", "CIP13": "", "DCI": ""},
        {"CIP7": "2345678", "CIP13": "", "DCI": "Test3"},
    ]
    with csv_file.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["CIP7", "CIP13", "DCI"], delimiter=";")
        writer.writeheader()
        writer.writerows(rows)
    mapping = loader._parse_csv(csv_file)
    assert mapping == {"1234567": "TEST1", "7654321": "TEST2", "2345678": "TEST3"}


def test_parse_sqlite(tmp_path):
    db_file = tmp_path / "test.sqlite"
    conn = sqlite3.connect(str(db_file))
    cur = conn.cursor()
    # create compositions and cips tables
    cur.execute("CREATE TABLE compositions (cis TEXT, substance TEXT)")
    cur.execute("INSERT INTO compositions VALUES (?, ?)", ("CIS1", "SUB1"))
    cur.execute("CREATE TABLE cips (cip TEXT, cis TEXT)")
    cur.execute("INSERT INTO cips VALUES (?, ?)", ("CIP1", "CIS1"))
    conn.commit()
    conn.close()
    # parse sqlite
    mapping = loader._parse_sqlite(db_file)
    # composition entries
    assert mapping["CIS1"] == "SUB1"
    assert mapping["SUB1"] == "SUB1"
    # cip lookup
    assert mapping["CIP1"] == "SUB1"


def test_get_mapping_priority(tmp_path, monkeypatch):
    # Case 1: SQLite exists
    sql_file = tmp_path / "bdpm.sqlite"
    sql_file.write_bytes(b"")
    monkeypatch.setattr(loader, 'DB_PATH', sql_file)
    monkeypatch.setattr(loader, 'CSV_PATH', tmp_path / "no.csv")
    # stub parse_sqlite
    monkeypatch.setattr(loader, '_parse_sqlite', lambda p: {'foo': 'BAR'})
    m1 = loader.get_mapping()
    assert m1 == {'foo': 'BAR'}
    loader.get_mapping.cache_clear()
    # Case 2: CSV exists
    monkeypatch.setattr(loader, 'DB_PATH', tmp_path / "no.db")
    csv_file = tmp_path / "bdpm_substances.csv"
    csv_file.write_text("CIP7;CIP13;DCI\n000;000;abc\n", encoding="utf-8")
    monkeypatch.setattr(loader, 'CSV_PATH', csv_file)
    # stub parse_csv
    monkeypatch.setattr(loader, '_parse_csv', lambda p: {'abc': 'ABC'})
    m2 = loader.get_mapping()
    assert m2 == {'abc': 'ABC'}
    loader.get_mapping.cache_clear()
    # Case 3: neither exists → fallback
    monkeypatch.setattr(loader, 'DB_PATH', tmp_path / "no.db2")
    monkeypatch.setattr(loader, 'CSV_PATH', tmp_path / "no.csv2")
    fb = loader.get_mapping()
    assert fb == loader._FALLBACK


def test_normalize_drug(tmp_path, monkeypatch):
    # stub get_mapping
    monkeypatch.setattr(loader, 'get_mapping', lambda: {'XXX': 'DDD'})
    assert loader.normalize_drug('xxx') == 'DDD'
    assert loader.normalize_drug('unknown') == 'UNKNOWN'
