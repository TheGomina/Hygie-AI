import pytest
import datetime as pydatetime
from backend.bmp_service import fhir_bundle as fb


def test_id(monkeypatch):
    # Stub uuid4 for deterministic ID
    monkeypatch.setattr(fb.uuid, 'uuid4', lambda: 'ID123')
    assert fb._id() == 'ID123'


class DummyDatetime:
    @classmethod
    def now(cls, tz):
        # Fixed timestamp using passed tzinfo
        return pydatetime.datetime(2020, 1, 1, 12, 0, 0, tzinfo=tz)


def test_build_bundle_full(monkeypatch):
    # Stub ID and now
    monkeypatch.setattr(fb.uuid, 'uuid4', lambda: 'UID')
    monkeypatch.setattr(fb, 'datetime', DummyDatetime)
    patient = {'age': 30, 'sex': 'F'}
    meds = ['m1', 'm2']
    issues = ['i1']
    recs = ['r1', 'r2']
    bundle = fb.build_bundle(patient, meds, issues, recs)

    assert bundle['resourceType'] == 'Bundle'
    assert bundle['type'] == 'collection'
    assert bundle['timestamp'] == '2020-01-01T12:00:00+00:00'
    assert bundle['id'] == 'UID'

    entries = bundle['entry']
    # 1 patient + 2 meds + 1 issue + 1 guidance
    assert len(entries) == 5
    types = [e['resource']['resourceType'] for e in entries]
    assert types == [
        'Patient',
        'MedicationStatement',
        'MedicationStatement',
        'DetectedIssue',
        'GuidanceResponse'
    ]

    # Patient resource checks
    patient_res = entries[0]['resource']
    assert patient_res['gender'] == 'female'
    # birthDate = 2025 - age
    assert patient_res['birthDate'] == '1995'


def test_build_bundle_no_recs(monkeypatch):
    # Stub ID and now
    monkeypatch.setattr(fb.uuid, 'uuid4', lambda: 'ID')
    monkeypatch.setattr(fb, 'datetime', DummyDatetime)
    patient = {'age': 50, 'sex': 'M'}
    bundle = fb.build_bundle(patient, [], [], [])

    entries = bundle['entry']
    # only patient
    assert len(entries) == 1
    patient_res = entries[0]['resource']
    assert patient_res['gender'] == 'male'
    assert patient_res['birthDate'] == '1975'
