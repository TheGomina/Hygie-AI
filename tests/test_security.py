import datetime
import pytest
from datetime import timezone
from jose import jwt
from backend.auth_service import security as sec
from backend.auth_service.security import hash_password, verify_password, create_access_token


def test_hash_and_verify_password():
    pwd = "StrongPass123"
    hashed = hash_password(pwd)
    assert hashed != pwd
    assert verify_password(pwd, hashed)
    assert not verify_password("WrongPass", hashed)


def test_create_access_token(monkeypatch):
    # Override secret, algorithm, and expire duration
    monkeypatch.setattr(sec, 'SECRET_KEY', 'testsecret')
    monkeypatch.setattr(sec, 'ALGORITHM', 'HS256')
    monkeypatch.setattr(sec, 'ACCESS_TOKEN_EXPIRE', datetime.timedelta(minutes=5))

    sub = 'user123'
    role = 'pro'
    token = create_access_token(sub, role)
    # Decode without verifying expiration
    claims = jwt.decode(token, sec.SECRET_KEY, algorithms=[sec.ALGORITHM])
    assert claims['sub'] == sub
    assert claims['role'] == role
    assert 'exp' in claims
    # exp should be a timestamp in the future
    now_ts = int(datetime.datetime.now(timezone.utc).timestamp())
    assert isinstance(claims['exp'], int)
    assert claims['exp'] > now_ts
