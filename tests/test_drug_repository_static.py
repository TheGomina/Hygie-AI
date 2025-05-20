import pytest

from backend.bmp_service.drug_repository import substances_from_atc, supported_atc_codes


def test_substances_from_atc_known():
    subs = substances_from_atc("C09AA")
    assert isinstance(subs, set)
    assert "LISINOPRIL" in subs


def test_substances_from_atc_unknown():
    assert substances_from_atc("UNKNOWN") == set()


def test_supported_atc_codes():
    codes = supported_atc_codes()
    assert isinstance(codes, list)
    assert all(isinstance(c, str) for c in codes)
    # Ensure some known code present
    assert "C09AA" in codes
