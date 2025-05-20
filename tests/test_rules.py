import pytest
from backend.bmp_service.rules import detect_interactions, _key, _INTERACTIONS


def test_no_interactions():
    # No or unknown medications
    assert detect_interactions([]) == []
    assert detect_interactions(["UNKNOWN_MED"]) == []


def test_single_interaction():
    meds = ["lisinopril", "ibuprofen"]
    key = _key(*meds)
    assert key in _INTERACTIONS
    expected = _INTERACTIONS[key]
    res = detect_interactions(meds)
    assert res == [expected]


def test_multiple_interactions():
    meds = ["ibuprofen", "lisinopril", "warfarin", "trimethoprim"]
    # Expected interactions: ibuprofen+lisinopril, warfarin+ibuprofen, warfarin+trimethoprim
    keys = {
        _key("ibuprofen", "lisinopril"),
        _key("warfarin", "ibuprofen"),
        _key("warfarin", "trimethoprim"),
    }
    expected = [ _INTERACTIONS[k] for k in keys ]
    res = detect_interactions(meds)
    # Order may vary; compare as sets and correct count
    assert set(res) == set(expected)
    assert len(res) == len(expected)


def test_duplicates_ignored():
    meds = ["ibuprofen", "lisinopril", "ibuprofen", "lisinopril"]
    res = detect_interactions(meds)
    # Should only detect once
    assert len(res) == 1
