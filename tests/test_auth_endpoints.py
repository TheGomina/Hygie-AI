import pytest
from backend.auth_service import app as auth_mod

class DummyColl:
    def __init__(self, find_one_ret=None, inserted_id=None):
        self.find_one_ret = find_one_ret
        self.inserted_id = inserted_id

    def find_one(self, *args, **kwargs):
        return self.find_one_ret

    def insert_one(self, doc):
        return type('R', (), {'inserted_id': self.inserted_id})


def test_metrics(auth_client):
    res = auth_client.get('/metrics')
    assert res.status_code == 200
    assert res.headers['content-type'] == 'text/plain; version=0.0.4; charset=utf-8'
    assert res.content == b''


def test_register_duplicate(auth_client, monkeypatch):
    dummy = DummyColl(find_one_ret={'email': 'a@example.com'})
    monkeypatch.setattr(auth_mod, 'users_col', lambda: dummy)
    res = auth_client.post(
        '/auth/register',
        json={'email': 'a@example.com', 'password': 'password1'}
    )
    assert res.status_code == 400
    assert res.json()['detail'] == 'Email already registered'


def test_register_success(auth_client, monkeypatch):
    dummy = DummyColl(find_one_ret=None, inserted_id='abc123')
    monkeypatch.setattr(auth_mod, 'users_col', lambda: dummy)
    monkeypatch.setattr(auth_mod, 'create_access_token', lambda sub, role: 'tok123')
    res = auth_client.post(
        '/auth/register',
        json={'email': 'new@example.com', 'password': 'password1'}
    )
    assert res.status_code == 201
    data = res.json()
    assert data['access_token'] == 'tok123'


def test_login_invalid(auth_client, monkeypatch):
    dummy = DummyColl(find_one_ret=None)
    monkeypatch.setattr(auth_mod, 'users_col', lambda: dummy)
    res = auth_client.post(
        '/auth/login',
        json={'email': 'noone@example.com', 'password': 'password1'}
    )
    assert res.status_code == 401
    assert res.json()['detail'] == 'Invalid credentials'


def test_login_wrong_password(auth_client, monkeypatch):
    dummy = DummyColl(find_one_ret={
        'email': 'user@example.com', 'hashed_password': 'hpass', 'role': 'pro', '_id': 'id1'
    })
    monkeypatch.setattr(auth_mod, 'users_col', lambda: dummy)
    monkeypatch.setattr(auth_mod, 'verify_password', lambda pw, hpw: False)
    res = auth_client.post(
        '/auth/login',
        json={'email': 'user@example.com', 'password': 'password1'}
    )
    assert res.status_code == 401
    assert res.json()['detail'] == 'Invalid credentials'


def test_login_success(auth_client, monkeypatch):
    dummy = DummyColl(find_one_ret={
        'email': 'user2@example.com', 'hashed_password': 'hpass', 'role': 'phi', '_id': 'id1'
    })
    monkeypatch.setattr(auth_mod, 'users_col', lambda: dummy)
    monkeypatch.setattr(auth_mod, 'verify_password', lambda pw, hpw: True)
    monkeypatch.setattr(auth_mod, 'create_access_token', lambda sub, role: 'tok456')
    res = auth_client.post(
        '/auth/login',
        json={'email': 'user2@example.com', 'password': 'password1'}
    )
    assert res.status_code == 200
    assert res.json()['access_token'] == 'tok456'
