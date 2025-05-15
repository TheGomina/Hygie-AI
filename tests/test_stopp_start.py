import pytest

from backend.bmp_service.stopp_start import evaluate_rules


def test_static_rule_A1_applicable():
    demo = {"age": 70, "sex": "F"}
    meds = ["DIPHENHYDRAMINE"]
    stops, starts = evaluate_rules(demo, meds)
    assert "Anticholinergic chez >65 ans" in stops
    assert starts == []


def test_static_rule_B2_applicable():
    demo = {"age": 50, "sex": "M"}
    meds = ["LISINOPRIL", "POTASSIUM"]
    stops, starts = evaluate_rules(demo, meds)
    assert "IEC + supplément de potassium" in stops
    assert starts == []


def test_no_rules_for_unknown_medication():
    demo = {"age": 80, "sex": "F"}
    meds = ["ASPIRIN"]
    stops, starts = evaluate_rules(demo, meds)
    assert stops == []
    assert starts == []
