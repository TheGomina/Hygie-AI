import pytest
import re
from backend.bmp_service.fhir_bundle import build_bundle, _id
from datetime import datetime, timezone


def test_id_format():
    uid = _id()
    # UUID v4 has 36 chars including hyphens
    assert isinstance(uid, str) and re.match(r"[0-9a-fA-F\-]{36}", uid)

@ pytest.fixture
def sample_data():
    patient = {"age": 30, "sex": "M"}
    meds = ["med1", "med2"]
    issues = ["issue1"]
    recs = ["rec1", "rec2"]
    return patient, meds, issues, recs


def test_build_bundle_with_recommendations(sample_data):
    patient, meds, issues, recs = sample_data
    bundle = build_bundle(patient, meds, issues, recs)
    assert bundle["resourceType"] == "Bundle"
    assert bundle["type"] == "collection"
    # entries: 1 patient + 2 meds + 1 issue + 1 guidance
    assert len(bundle["entry"]) == 5
    # Check patient entry
    patient_entry = bundle["entry"][0]["resource"]
    assert patient_entry["resourceType"] == "Patient"
    # gender male since sex M
    assert patient_entry["gender"] == "male"
    # birthDate is year string of birth = 2025 - age
    year = int(patient_entry["birthDate"])
    assert 2025 - patient["age"] == year
    # Check GuidanceResponse last entry
    guidance = bundle["entry"][-1]["resource"]
    assert guidance["resourceType"] == "GuidanceResponse"
    # outputParameters.text matches joined recs
    assert guidance["outputParameters"]["text"] == "rec1\nrec2"


def test_build_bundle_without_recommendations(sample_data):
    patient, meds, issues, _ = sample_data
    bundle = build_bundle(patient, meds, issues, [])
    # entries: 1 + 2 + 1 = 4
    assert len(bundle["entry"]) == 4
    # ensure no GuidanceResponse resource
    types = [e["resource"]["resourceType"] for e in bundle["entry"]]
    assert "GuidanceResponse" not in types
