import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from fastapi.testclient import TestClient

from backend.auth_service.app import app

client = TestClient(app)


def test_register_and_login():
    email = "test@example.com"
    pwd = "secret123"
    # register
    r = client.post("/auth/register", json={"email": email, "password": pwd})
    assert r.status_code == 201
    token = r.json()["access_token"]
    assert token
    # duplicate register
    r2 = client.post("/auth/register", json={"email": email, "password": pwd})
    assert r2.status_code == 400
    # login
    r3 = client.post("/auth/login", json={"email": email, "password": pwd})
    assert r3.status_code == 200
    assert r3.json()["access_token"]
