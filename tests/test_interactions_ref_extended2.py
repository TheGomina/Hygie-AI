import pytest
import csv
from pathlib import Path

from backend.bmp_service import interactions_ref as ir


def test_load_from_csv(tmp_path):
    # Valid CSV: include rows with severity C and D
    fp = tmp_path / "inter.csv"
    rows = [["A","B","C"], ["C","D","B"], ["E","F","D"]]
    with fp.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh, delimiter=";")
        for r in rows:
            writer.writerow(r)
    mapping = ir._load_from_csv(fp)
    assert ("A","B") in mapping
    assert ("E","F") in mapping
    assert ("C","D") not in mapping  # severity B filtered out


def test_load_from_csv_malformed(tmp_path):
    # Malformed CSV should return empty
    fp = tmp_path / "bad.csv"
    fp.write_text("not,a,csv", encoding="utf-8")
    mapping = ir._load_from_csv(fp)
    assert mapping == {}


def test_load_from_pdf(tmp_path, monkeypatch):
    # Stub extract_text to return lines matching pair pattern
    fp = tmp_path / "file.pdf"
    fp.write_text("", encoding="utf-8")
    # patch module-level extract_text
    monkeypatch.setattr(ir, 'extract_text', lambda x: "X / Y\ninvalid line")
    mapping = ir._load_from_pdf(fp)
    assert ("X","Y") in mapping
    assert mapping[("X","Y")].startswith("Interaction ANSM")


def test_get_interactions_csv_priority(tmp_path, monkeypatch):
    # CSV present in resources
    res = tmp_path / "res"
    res.mkdir()
    csv_fp = res / "test_interaction.csv"
    with csv_fp.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh, delimiter=";")
        writer.writerow(["M1","M2","C"])
    monkeypatch.setattr(ir, "_RES", res)
    ir.get_interactions.cache_clear()
    mapping = ir.get_interactions()
    assert ("M1","M2") in mapping


def test_get_interactions_fallback_pdf(tmp_path, monkeypatch):
    # No CSV, but PDF present
    res = tmp_path / "res"
    res.mkdir()
    pdf_fp = res / "test_interaction.pdf"
    pdf_fp.write_text("", encoding="utf-8")
    monkeypatch.setattr(ir, "_RES", res)
    # patch extract_text for PDF fallback
    monkeypatch.setattr(ir, 'extract_text', lambda x: "P / Q")
    ir.get_interactions.cache_clear()
    mapping = ir.get_interactions()
    assert ("P","Q") in mapping


def test_get_interactions_empty(tmp_path, monkeypatch):
    # No CSV or PDF
    res = tmp_path / "res"
    res.mkdir()
    monkeypatch.setattr(ir, "_RES", res)
    ir.get_interactions.cache_clear()
    mapping = ir.get_interactions()
    assert mapping == {}
