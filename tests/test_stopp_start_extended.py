import pytest
from backend.bmp_service.stopp_start import (
    evaluate_rules, Rule, has_med, _load_yaml_rules, _rule_A1, _rule_B2
)
import backend.bmp_service.stopp_start as ss


def test_rule_applies_missing_age():
    r = Rule("A1", "desc", _rule_A1)
    with pytest.raises(AssertionError) as exc:
        r.applies({}, ["DIPHENHYDRAMINE"])
    assert "Missing age in demographics" in str(exc.value)


def test_rule_applies_wrong_meds_type():
    r = Rule("B2", "desc", _rule_B2)
    with pytest.raises(AssertionError) as exc:
        r.applies({"age": 50}, "not_a_list")
    assert "meds must be list" in str(exc.value)


def test_rule_A1_predicate():
    # Direct predicate calls
    assert _rule_A1({"age": 70}, ["DIPHENHYDRAMINE"]) is True
    assert _rule_A1({"age": 70}, []) is False


def test_rule_A1_boundary():
    # Age exactly 65
    stops, starts = evaluate_rules({"age": 65}, ["OXYBUTYNIN"])
    assert "Anticholinergic chez >65 ans" in stops
    assert starts == []


def test_rule_A1_not_applicable_under65():
    stops, _ = evaluate_rules({"age": 64}, ["DIPHENHYDRAMINE"])
    assert stops == []


def test_rule_B2_predicate():
    assert _rule_B2({}, ["LISINOPRIL", "KCL"]) is True
    assert _rule_B2({}, ["RAMIPRIL"]) is False
    assert _rule_B2({}, ["KCL"]) is False


def test_has_med_case_insensitive():
    assert has_med(["aspirin", "lisinopril"], {"LISINOPRIL"})
    assert not has_med(["aspirin"], {"LISINOPRIL", "IBUPROFEN"})


def test_load_yaml_rules_no_file(monkeypatch):
    # simulate missing YAML file
    monkeypatch.setattr(ss.Path, "exists", lambda self: False)
    rules = ss._load_yaml_rules()
    assert isinstance(rules, list)
    assert rules == []
