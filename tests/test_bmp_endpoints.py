import pytest
from backend.bmp_service import main as main_mod

# Sample BMPRequest payload
BMP_PAYLOAD = {
    "patient_id": "p1",
    "demographics": {"age": 30, "sex": "M"},
    "medications": [{"name": "med1"}],
}

@pytest.mark.parametrize("token_fixture,expected_status", [
    (None, 401),
    ("patient_token", 200),
    ("pro_token", 200),
])
def test_run_bmp_auth(bmp_client, request, monkeypatch, token_fixture, expected_status):
    # Stub pipeline functions for deterministic output
    monkeypatch.setattr(main_mod, 'detect_interactions', lambda meds: ['I'])
    monkeypatch.setattr(main_mod, 'evaluate_rules', lambda demo, meds: (['stop'], ['start']))
    monkeypatch.setattr(main_mod, 'compute_burden_score', lambda meds: 4)
    monkeypatch.setattr(main_mod, 'generate_summary', lambda demo, meds, problems: 'sum')

    headers = {}
    if token_fixture:
        token = request.getfixturevalue(token_fixture)
        headers = {"Authorization": f"Bearer {token}"}
    res = bmp_client.post('/bmp/run', json=BMP_PAYLOAD, headers=headers)
    assert res.status_code == expected_status
    if expected_status == 200:
        data = res.json()
        assert data['status'] == 'ok'
        # problems: detect_interactions + stops + burden issue
        assert 'I' in data['problems']
        assert 'stop' in data['problems']
        assert any('anticholinergique élevée' in p for p in data['problems'])
        # recommendations starts with summary
        assert data['recommendations'][0] == 'sum'
        # start rule in recommendations
        assert 'start' in data['recommendations']


def test_run_bmp_fhir(bmp_client, pro_token, monkeypatch):
    # Stub as above
    monkeypatch.setattr(main_mod, 'detect_interactions', lambda meds: [])
    monkeypatch.setattr(main_mod, 'evaluate_rules', lambda demo, meds: ([], []))
    monkeypatch.setattr(main_mod, 'compute_burden_score', lambda meds: 0)
    monkeypatch.setattr(main_mod, 'generate_summary', lambda demo, meds, problems: 'summary')

    headers = {"Authorization": f"Bearer {pro_token}"}
    res = bmp_client.post('/bmp/fhir', json=BMP_PAYLOAD, headers=headers)
    assert res.status_code == 200
    bundle = res.json()
    assert bundle['resourceType'] == 'Bundle'
    # Only patient entry when no meds/issues
    entries = bundle['entry']
    assert entries[0]['resource']['resourceType'] == 'Patient'


def test_fhir_metadata():
    from backend.bmp_service.main import app
    from fastapi.testclient import TestClient
    client = TestClient(app)
    res = client.get('/fhir/metadata')
    assert res.status_code == 200
    data = res.json()
    assert data['resourceType'] == 'CapabilityStatement'
    assert 'fhirVersion' in data


def test_fhir_bmp_bundle(bmp_client, patient_token, monkeypatch):
    # Prepare input FHIR Bundle
    bundle_in = {'entry': [{'resource': {'resourceType': 'Bundle', 'id': 'x'}}]}
    # Stub parse_input_bundle to return demo and meds
    monkeypatch.setattr(main_mod, '_parse_input_bundle', lambda b: ({'age': 20, 'sex': 'F'}, ['m']))
    # Stub run_bmp to return object with problems & recommendations asynchronously
    class DummyRes:
        problems = ['p1']
        recommendations = ['r1']
    async def dummy_run_bmp(req, user):
        return DummyRes()
    monkeypatch.setattr(main_mod, 'run_bmp', dummy_run_bmp)
    headers = {"Authorization": f"Bearer {patient_token}"}
    res = bmp_client.post('/fhir/Bundle/$bmp', json=bundle_in, headers=headers)
    assert res.status_code == 200
    out = res.json()
    # Should be FHIR bundle
    assert out['resourceType'] == 'Bundle'


def test_models_access(bmp_client, patient_token):
    res = bmp_client.get('/models', headers={"Authorization": f"Bearer {patient_token}"})
    assert res.status_code == 200
    assert 'models' in res.json()


def test_chat_forbidden(bmp_client, patient_token):
    res = bmp_client.post('/chat', json={'prompt': 'hi'}, headers={"Authorization": f"Bearer {patient_token}"})
    assert res.status_code == 403
