import pytest
import asyncio
import datetime
from datetime import timezone, timedelta
from jose import jwt
from fastapi import HTTPException
from backend.bmp_service.security import (
    _decode_token, get_current_user_id, get_current_user,
    require_role, CurrentUser, SECRET_KEY, ALGORITHM
)


def test_decode_token_invalid():
    assert _decode_token("not_a_token") is None


def test_decode_token_valid():
    exp = datetime.datetime.now(timezone.utc) + timedelta(hours=1)
    token = jwt.encode({"sub": "user1", "role": "patient", "exp": exp}, SECRET_KEY, algorithm=ALGORITHM)
    assert _decode_token(token) == "user1"

def test_get_current_user_id_success():
    exp = datetime.datetime.now(timezone.utc) + timedelta(hours=1)
    token = jwt.encode({"sub": "u123", "role": "patient", "exp": exp}, SECRET_KEY, algorithm=ALGORITHM)
    uid = asyncio.run(get_current_user_id(token))
    assert uid == "u123"

def test_get_current_user_id_fail():
    with pytest.raises(HTTPException) as ei:
        asyncio.run(get_current_user_id("bad"))
    assert ei.value.status_code == 401

def test_get_current_user_success():
    exp = datetime.datetime.now(timezone.utc) + timedelta(hours=1)
    token = jwt.encode({"sub": "u456", "role": "pro", "exp": exp}, SECRET_KEY, algorithm=ALGORITHM)
    user = asyncio.run(get_current_user(token))
    assert isinstance(user, CurrentUser)
    assert user.id == "u456" and user.role == "pro"

def test_get_current_user_missing_payload():
    exp = datetime.datetime.now(timezone.utc) + timedelta(hours=1)
    token = jwt.encode({"sub": "u789", "exp": exp}, SECRET_KEY, algorithm=ALGORITHM)
    with pytest.raises(HTTPException) as ei:
        asyncio.run(get_current_user(token))
    assert ei.value.status_code == 401


def test_require_role_ok():
    dep = require_role("pro")
    cu = CurrentUser(id="1", role="pro")
    assert dep(cu) == cu


def test_require_role_forbidden():
    dep = require_role("pro")
    cu = CurrentUser(id="2", role="patient")
    with pytest.raises(HTTPException) as e:
        dep(cu)
    assert e.value.status_code == 403
