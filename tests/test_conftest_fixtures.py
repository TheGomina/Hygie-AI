import pytest

# Use fixtures from conftest

def test_clients_and_tokens(auth_client, bmp_client, pro_token, patient_token):
    # Test auth_client /metrics endpoint
    r = auth_client.get("/metrics")
    assert r.status_code == 200
    # Test bmp_client OpenAPI
    r2 = bmp_client.get("/openapi.json")
    assert r2.status_code == 200
    assert b"paths" in r2.content
    # Tokens
    assert isinstance(pro_token, str) and len(pro_token) > 0
    assert isinstance(patient_token, str) and len(patient_token) > 0
