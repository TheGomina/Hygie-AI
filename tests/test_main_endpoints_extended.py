import pytest
from fastapi.testclient import TestClient
from fastapi import HTTPException
from backend.bmp_service.main import app

# Standalone client (no auth)
client = TestClient(app)

# New payload for BMP tests
BMP_PAYLOAD = {
    "patient_id": "p1",
    "demographics": {"age": 30, "sex": "M"},
    "medications": [{"name": "med1"}],
}

def test_metrics_endpoint():
    res = client.get("/metrics")
    assert res.status_code == 200
    ctype = res.headers.get("content-type", "")
    assert ctype.startswith("text/plain")
    # metrics body may be empty if no requests have been recorded


def test_models_no_token(bmp_client):
    res = bmp_client.get("/models")
    assert res.status_code == 401


def test_models_success(monkeypatch, bmp_client, pro_token):
    import backend.bmp_service.main as main_mod
    monkeypatch.setattr(main_mod, 'list_local_models', lambda: ['a','b'])
    headers = {'Authorization': f'Bearer {pro_token}'}
    res = bmp_client.get('/models', headers=headers)
    assert res.status_code == 200
    assert res.json() == {'models': ['a','b']}


def test_chat_success(bmp_client, pro_token):
    res = bmp_client.post(
        "/chat", json={"prompt": "hello"}, headers={"Authorization": f"Bearer {pro_token}"}
    )
    assert res.status_code == 200
    assert res.json() == {"response": "LLM stub"}


def test_chat_unauthorized(monkeypatch, bmp_client, patient_token):
    headers = {'Authorization': f'Bearer {patient_token}'}
    res = bmp_client.post('/chat', json={'prompt': 'hi'}, headers=headers)
    assert res.status_code == 403


def test_fhir_bmp_bundle_error(monkeypatch, bmp_client, patient_token):
    # Stub parse_input_bundle to throw HTTPException
    from backend.bmp_service import main as main_mod

    def bad_parse(b):
        raise HTTPException(400, "Bad input")

    monkeypatch.setattr(main_mod, "_parse_input_bundle", bad_parse)
    headers = {"Authorization": f"Bearer {patient_token}"}
    res = bmp_client.post(
        '/fhir/Bundle/$bmp', json={}, headers=headers
    )
    assert res.status_code == 400
    assert res.json().get("detail") == "Bad input"


def test_fhir_bmp_bundle_success(monkeypatch, bmp_client, patient_token):
    import backend.bmp_service.main as main_mod
    monkeypatch.setattr(main_mod, '_parse_input_bundle', lambda b: ({'age':30,'sex':'M'}, ['MED1']))
    monkeypatch.setattr(main_mod, 'detect_interactions', lambda meds: [])
    monkeypatch.setattr(main_mod, 'evaluate_rules', lambda demo, meds: ([], ['START1']))
    monkeypatch.setattr(main_mod, 'compute_burden_score', lambda meds: 4)
    monkeypatch.setattr(main_mod, 'generate_summary', lambda demo, meds, probs: 'SUM')
    headers = {'Authorization': f'Bearer {patient_token}'}
    res = bmp_client.post('/fhir/Bundle/$bmp', json={}, headers=headers)
    assert res.status_code == 200
    bundle = res.json()
    assert bundle['resourceType'] == 'Bundle'


# New tests for /bmp/run and /bmp/fhir validation and error scenarios
def test_bmp_run_unauthenticated():
    res = client.post("/bmp/run", json=BMP_PAYLOAD)
    assert res.status_code == 401

def test_bmp_run_validation_error(bmp_client, patient_token):
    invalid_payload = {"patient_id": "p1", "demographics": {"age": 30, "sex": "M"}}
    headers = {"Authorization": f"Bearer {patient_token}"}
    res = bmp_client.post("/bmp/run", json=invalid_payload, headers=headers)
    assert res.status_code == 422

def test_bmp_run_internal_error(monkeypatch, bmp_client, pro_token):
    import backend.bmp_service.main as main_mod
    monkeypatch.setattr(main_mod, "detect_interactions", lambda meds: (_ for _ in ()).throw(Exception("oops")))
    headers = {"Authorization": f"Bearer {pro_token}"}
    res = bmp_client.post("/bmp/run", json=BMP_PAYLOAD, headers=headers)
    assert res.status_code == 500

def test_bmp_run_success(monkeypatch, bmp_client, pro_token):
    import backend.bmp_service.main as main_mod
    monster = main_mod
    monkeypatch.setattr(main_mod, 'detect_interactions', lambda meds: ['I1'])
    monkeypatch.setattr(main_mod, 'evaluate_rules', lambda demo, meds: (['S1'], ['START1']))
    monkeypatch.setattr(main_mod, 'compute_burden_score', lambda meds: 0)
    monkeypatch.setattr(main_mod, 'generate_summary', lambda demo, meds, probs: 'SUM')
    headers = {'Authorization': f'Bearer {pro_token}'}
    res = bmp_client.post('/bmp/run', json=BMP_PAYLOAD, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data['status'] == 'ok'
    assert 'SUM' in data['recommendations']

def test_bmp_fhir_validation_error(bmp_client, patient_token):
    headers = {"Authorization": f"Bearer {patient_token}"}
    res = bmp_client.post("/bmp/fhir", json={}, headers=headers)
    assert res.status_code == 422

def test_fhir_metadata(bmp_client, patient_token):
    headers = {'Authorization': f'Bearer {patient_token}'}
    res = bmp_client.get('/fhir/metadata', headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data['resourceType'] == 'CapabilityStatement'

def test_bmp_run_strategy_ensemble(monkeypatch, bmp_client, pro_token):
    import backend.bmp_service.main as main_mod
    # ensure generate_summary receives correct model_name
    def fake_gen(demo, meds, probs, model_name):
        assert model_name == 'ensemble'
        return 'ENS'
    monkeypatch.setattr(main_mod, 'generate_summary', fake_gen)
    headers = {'Authorization': f'Bearer {pro_token}'}
    res = bmp_client.post("/bmp/run?strategy=ensemble", json=BMP_PAYLOAD, headers=headers)
    assert res.status_code == 200
    assert 'ENS' in res.json()['recommendations']

def test_bmp_run_strategy_cascade(monkeypatch, bmp_client, pro_token):
    import backend.bmp_service.main as main_mod
    def fake_gen(demo, meds, probs, model_name):
        assert model_name == 'cascade'
        return 'CAS'
    monkeypatch.setattr(main_mod, 'generate_summary', fake_gen)
    headers = {'Authorization': f'Bearer {pro_token}'}
    res = bmp_client.post("/bmp/run?strategy=cascade", json=BMP_PAYLOAD, headers=headers)
    assert res.status_code == 200
    assert 'CAS' in res.json()['recommendations']

def test_fhir_bmp_bundle_strategy_ensemble(monkeypatch, bmp_client, patient_token):
    import backend.bmp_service.main as main_mod
    # stub parsing and pipeline
    monkeypatch.setattr(main_mod, '_parse_input_bundle', lambda b: ({'age':1,'sex':'F'}, ['MED']))
    monkeypatch.setattr(main_mod, 'detect_interactions', lambda meds: [])
    monkeypatch.setattr(main_mod, 'evaluate_rules', lambda demo, meds: ([], []))
    monkeypatch.setattr(main_mod, 'compute_burden_score', lambda meds: 0)
    def fake_gen(demo, meds, probs, model_name):
        assert model_name == 'ensemble'
        return 'ENSUM'
    monkeypatch.setattr(main_mod, 'generate_summary', fake_gen)
    headers = {'Authorization': f'Bearer {patient_token}'}
    res = bmp_client.post("/fhir/Bundle/$bmp?strategy=ensemble", json={}, headers=headers)
    assert res.status_code == 200
    bundle = res.json()
    # ensure recommendation ENSUM appears in GuidanceResponse
    gr_texts = [entry['resource']['outputParameters']['text'] for entry in bundle['entry'] if entry['resource']['resourceType'] == 'GuidanceResponse']
    assert any('ENSUM' in t for t in gr_texts)
