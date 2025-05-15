from pathlib import Path

import pytest

import backend.bmp_service.stopp_start as ss
from backend.bmp_service.stopp_start import Rule, _load_yaml_rules, has_med


def test_has_med_true_false():
    assert has_med(["aspirin", "IbUpRoFeN"], {"ASPIRIN"}) is True
    assert not has_med(["paracetamol"], {"IBUPROFEN"})


def test_has_med_assertion():
    with pytest.raises(AssertionError):
        has_med(["aspirin"], ["not", "a", "set"])


def test_load_yaml_rules_no_file(monkeypatch):
    # Simulate missing YAML file
    monkeypatch.setattr(ss.Path, "exists", lambda self: False)
    assert _load_yaml_rules() == []


def test_load_yaml_rules_disabled(monkeypatch):
    # Simulate YAML with a disabled rule
    data = [{"id": "X", "description": "desc", "substances": [], "enabled": False}]
    monkeypatch.setattr(ss.Path, "exists", lambda self: True)
    monkeypatch.setattr(ss.yaml, "safe_load", lambda fh: data)
    rules = _load_yaml_rules()
    assert rules == []


def test_load_yaml_rules_predicate_and_phase(monkeypatch):
    # Simulate YAML with an active START rule
    data = [
        {
            "id": "Z1",
            "description": "desc z1",
            "substances": ["A"],
            "age_min": 10,
            "age_max": 20,
            "enabled": True,
            "phase": "START",
        }
    ]
    monkeypatch.setattr(ss.Path, "exists", lambda self: True)
    monkeypatch.setattr(ss.yaml, "safe_load", lambda fh: data)
    rules = _load_yaml_rules()
    assert len(rules) == 1
    rule = rules[0]
    assert isinstance(rule, Rule)
    assert rule.id == "Z1"
    assert rule.phase == "START"
    # Test predicate logic
    assert rule.predicate({"age": 15}, ["A"]) is True
    assert rule.predicate({"age": 9}, ["A"]) is False
    assert rule.predicate({"age": 15}, ["B"]) is False


def test_load_yaml_rules_with_atc(monkeypatch):
    # Simulate YAML with an ATC-based rule
    data = [
        {
            "id": "Y1",
            "description": "desc y1",
            "atc": "C10AA",
            "age_min": 0,
            "age_max": 100,
            "enabled": True,
            "phase": "STOPP",
        }
    ]
    monkeypatch.setattr(ss.Path, "exists", lambda self: True)
    monkeypatch.setattr(ss.yaml, "safe_load", lambda fh: data)
    # Mock ATC mapping
    monkeypatch.setattr(ss, "substances_from_atc", lambda code: {"X", "Y"})
    rules = _load_yaml_rules()
    assert len(rules) == 1
    rule = rules[0]
    assert rule.id == "Y1"
    # Predicate should use mocked substances
    assert rule.predicate({"age": 50}, ["X"]) is True
    assert rule.predicate({"age": 50}, ["Z"]) is False
