"""Unit tests for Hygie-AI BMP backend.
Run with:  pytest backend -q
"""

import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from backend.bmp_service import main as main_mod  # type: ignore
from backend.bmp_service import rules as rules_mod  # type: ignore
from backend.bmp_service import stopp_start as ss_mod  # type: ignore


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setattr(main_mod, "generate_summary", lambda *_, **__: "[stub LLM]")
    return TestClient(main_mod.app)


def test_detect_interactions():
    meds = ["LISINOPRIL", "Ibuprofen", "Paracetamol"]
    found = rules_mod.detect_interactions(meds)
    assert any("IEC" in p for p in found)
    assert not any("Paracetamol" in p for p in found)


def test_stopp_start_rules_A1_B2():
    demo = {"age": 70}
    meds = ["diphenhydramine", "lisinopril", "KCl"]
    stops, starts = ss_mod.evaluate_rules(demo, meds)
    probs = stops + starts
    joined = " ".join(probs)
    assert "Anticholinergic" in joined
    assert "IEC" in joined


def test_api_run_bmp(client):
    payload = {
        "patient_id": "t1",
        "demographics": {"age": 70, "sex": "F"},
        "medications": [
            {"name": "Lisinopril"},
            {"name": "Ibuprofen"},
            {"name": "KCl"},
        ],
    }
    resp = client.post("/bmp/run", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "ok"
    assert any("IEC" in p and "AINS" in p for p in data["problems"])


# ---------- New test for YAML-loaded rule ----------------------------------


def test_yaml_rule_A4():
    demo = {"age": 70}
    meds = ["Imipramine"]
    stops, starts = ss_mod.evaluate_rules(demo, meds)
    probs = stops + starts
    joined = " ".join(probs)
    assert "tricyclique" in joined or "tricyclic" in joined


# ---------- Test statine + macrolide via ATC mapping -----------------------


def test_interaction_statine_macrolide():
    probs = rules_mod.detect_interactions(["Atorvastatin", "Clarithromycin"])
    assert any("rhabdomyolyse" in p.lower() for p in probs)

    # Should also trigger STOPP/START B5 rule (statine + macrolide)
    demo = {"age": 68}
    stops2, starts2 = ss_mod.evaluate_rules(demo, ["Atorvastatin", "Clarithromycin"])
    probs2 = stops2 + starts2
    assert probs2  # non-empty


# ---------- Test ATC-based rule M1 ----------------------------------------


def test_rule_M1_atc():
    demo = {"age": 85}
    meds = ["Morphine"]  # part of N02AA class
    stops, starts = ss_mod.evaluate_rules(demo, meds)
    probs = stops + starts
    assert any("Opioïde" in p or "opioï" in p for p in probs)


# ---------- Performance sanity check (<=100 ms) ---------------------------


def test_performance_bulk():
    big_list = [
        "Ibuprofen",
        "Lisinopril",
        "KCl",
        "Atorvastatin",
        "Clarithromycin",
        "Morphine",
        "Diazepam",
        "Azithromycin",
        "Losartan",
        "Furosemide",
        "Apixaban",
        "Hydromorphone",
        "Piroxicam",
        "Ramipril",
        "Oxycodone",
        "Naproxen",
    ]
    import time

    t0 = time.perf_counter()
    _ = ss_mod.evaluate_rules({"age": 70}, big_list)
    _ = rules_mod.detect_interactions(big_list)
    elapsed = (time.perf_counter() - t0) * 1000
    assert elapsed < 100  # ms


# ---------- Additional utility tests to push coverage ----------------------


def test_substances_from_atc_helper():
    from backend.bmp_service.drug_repository import substances_from_atc

    statins = substances_from_atc("C10AA")
    assert "ATORVASTATIN" in statins and "SIMVASTATIN" in statins


def test_yaml_disabled_rule_not_triggered():
    # Rule K3 (AINS + ulcère) is disabled in YAML.
    demo = {"age": 70}
    meds = ["Ibuprofen", "Ketoprofen", "Piroxicam"]
    stops, starts = ss_mod.evaluate_rules(demo, meds)
    probs = stops + starts
    assert not any("ulcère" in p.lower() for p in probs)


def test_interaction_no_duplicates():
    meds = [
        "Lisinopril",
        "Ibuprofen",
        "Ibuprofen",  # duplicate
    ]
    probs = rules_mod.detect_interactions(meds)
    # Should list interaction once only
    assert len(probs) == 1
