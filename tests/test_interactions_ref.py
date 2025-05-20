import pytest
from pathlib import Path
from backend.bmp_service.interactions_ref import _load_from_csv, _order

def test_load_from_csv_filters_gravities(tmp_path):
    # Create a CSV with various gravities, only C and D should be kept
    content = (
        "ASPIRIN;Ibuprofen;C\n"
        "Paracetamol;Aspirin;D\n"
        "Morphine;Codeine;E\n"
        "; ;C\n"
    )
    csv_file = tmp_path / "test.csv"
    csv_file.write_text(content, encoding='utf-8')
    mapping = _load_from_csv(csv_file)
    expected = {
        _order('ASPIRIN', 'Ibuprofen'): "Interaction ANSM (C)",
        _order('Paracetamol', 'Aspirin'): "Interaction ANSM (D)"
    }
    assert mapping == expected

def test_load_from_csv_invalid_format(tmp_path):
    # Invalid CSV should return empty mapping
    csv_file = tmp_path / "invalid.csv"
    csv_file.write_text("not;a;valid;format", encoding='utf-8')
    assert _load_from_csv(csv_file) == {}
