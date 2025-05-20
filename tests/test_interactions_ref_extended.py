import pytest
from pathlib import Path
from backend.bmp_service.interactions_ref import (
    _load_from_csv, _load_from_pdf, get_interactions, _order
)


def setup_function():
    # Clear cache before each test
    get_interactions.cache_clear()


def test_load_from_csv_basic(tmp_path):
    # CSV with header and valid C/D rows
    fp = tmp_path / "int.csv"
    fp.write_text(
        "a;b;X\n"
        "X;Y;C\n"
        "A;B;D\n"
        ";;C\n",
        encoding='utf-8'
    )
    mapping = _load_from_csv(fp)
    expected = {
        tuple(sorted(("X", "Y"))): "Interaction ANSM (C)",
        tuple(sorted(("A", "B"))): "Interaction ANSM (D)",
    }
    assert mapping == expected


def test_load_from_csv_malformed(tmp_path):
    fp = tmp_path / "bad.csv"
    fp.write_text("not;a;csv;data", encoding='utf-8')
    mapping = _load_from_csv(fp)
    assert mapping == {}


def test_load_from_pdf(tmp_path, monkeypatch):
    fp = tmp_path / "foo.pdf"
    fp.write_text("", encoding='utf-8')
    # Stub extract_text to return lines
    monkeypatch.setattr(
        "backend.bmp_service.interactions_ref.extract_text",
        lambda x: "One / Two\nNO MATCH\nThree /Four "
    )
    mapping = _load_from_pdf(fp)
    # Keys are uppercased and sorted
    expected = {
        _order("One", "Two"): "Interaction ANSM (gravité non classée)",
        _order("Three", "Four"): "Interaction ANSM (gravité non classée)",
    }
    assert mapping == expected


def test_get_interactions_csv_priority(tmp_path, monkeypatch):
    # CSV exists: should return CSV mapping
    monkeypatch.setattr("backend.bmp_service.interactions_ref._RES", tmp_path)
    # Create dummy file
    (tmp_path / "x_interaction.csv").write_text("", encoding='utf-8')
    # Stub loader
    monkeypatch.setattr(
        "backend.bmp_service.interactions_ref._load_from_csv",
        lambda fp: {("A", "B"): "X"}
    )
    mapping = get_interactions()
    assert mapping == {("A", "B"): "X"}


def test_get_interactions_pdf_when_no_csv(tmp_path, monkeypatch):
    # No CSV, stub CSV loader empty, PDF exists
    monkeypatch.setattr("backend.bmp_service.interactions_ref._RES", tmp_path)
    (tmp_path / "x_interaction.pdf").write_text("", encoding='utf-8')
    monkeypatch.setattr(
        "backend.bmp_service.interactions_ref._load_from_csv",
        lambda fp: {}
    )
    monkeypatch.setattr(
        "backend.bmp_service.interactions_ref._load_from_pdf",
        lambda fp: {("C", "D"): "Y"}
    )
    mapping = get_interactions()
    assert mapping == {("C", "D"): "Y"}
    get_interactions.cache_clear()

def test_get_interactions_empty(tmp_path, monkeypatch):
    # No CSV or PDF → empty mapping
    monkeypatch.setattr("backend.bmp_service.interactions_ref._RES", tmp_path)
    mapping = get_interactions()
    assert mapping == {}
