import pytest
from fastapi.testclient import TestClient

# Import apps and token generation utility
from backend.auth_service.security import create_access_token
from backend.auth_service.app import app as auth_app
from backend.bmp_service.main import app as bmp_app

@pytest.fixture(scope="session")
def auth_client():
    return TestClient(auth_app)

@pytest.fixture(scope="session")
def bmp_client():
    return TestClient(bmp_app)

@pytest.fixture
def pro_token():
    # professional role token
    return create_access_token(subject="test-pro", role="pro")

@pytest.fixture
def patient_token():
    return create_access_token(subject="test-patient", role="patient")

# Unprotected endpoints

def test_metrics_unprotected(bmp_client):
    res = bmp_client.get("/metrics")
    assert res.status_code == 200


def test_fhir_metadata_unprotected(bmp_client):
    res = bmp_client.get("/fhir/metadata")
    assert res.status_code == 200
    assert res.json().get("resourceType") == "CapabilityStatement"

# /models endpoint

def test_models_requires_auth(bmp_client):
    res = bmp_client.get("/models")
    assert res.status_code == 401


def test_models_with_patient_token(bmp_client, patient_token):
    res = bmp_client.get(
        "/models", headers={"Authorization": f"Bearer {patient_token}"}
    )
    assert res.status_code == 200
    assert "models" in res.json()

# /chat endpoint

def test_chat_requires_pro(bmp_client, patient_token):
    res = bmp_client.post(
        "/chat", json={"prompt": "Hello"}, headers={"Authorization": f"Bearer {patient_token}"}
    )
    assert res.status_code == 403


def test_chat_with_pro_token(bmp_client, pro_token):
    res = bmp_client.post(
        "/chat", json={"prompt": "Hello"}, headers={"Authorization": f"Bearer {pro_token}"}
    )
    assert res.status_code == 200
    assert "response" in res.json()

# /bmp/run endpoint

def test_bmp_run_requires_auth(bmp_client):
    res = bmp_client.post("/bmp/run", json={})
    assert res.status_code == 401


def test_bmp_run_validation_error(bmp_client, pro_token):
    res = bmp_client.post(
        "/bmp/run", headers={"Authorization": f"Bearer {pro_token}"}, json={}
    )
    assert res.status_code == 422

# /bmp/fhir endpoint

def test_bmp_fhir_requires_auth(bmp_client):
    res = bmp_client.post("/bmp/fhir", json={})
    assert res.status_code == 401


def test_bmp_fhir_validation_error(bmp_client, pro_token):
    res = bmp_client.post(
        "/bmp/fhir", headers={"Authorization": f"Bearer {pro_token}"}, json={}
    )
    assert res.status_code == 422
