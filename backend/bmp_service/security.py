"""JWT validation helpers for BMP service.

We share the same secret key with the Auth service so we can verify
access tokens locally without an extra network round-trip.
"""

from __future__ import annotations

import os
from typing import Optional

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt

# Environment variables (fallback dev defaults)
SECRET_KEY: str = os.getenv("AUTH_SECRET_KEY", "devsecret")
ALGORITHM: str = os.getenv("AUTH_ALGORITHM", "HS256")

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


def _decode_token(token: str) -> Optional[str]:
    """Return subject (user id) if token valid, else None."""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
    except JWTError:
        return None
    return payload.get("sub")


async def get_current_user_id(token: str = Depends(oauth2_scheme)) -> str:  # noqa: D401
    """FastAPI dependency that returns the user id from JWT or raises 401."""
    user_id = _decode_token(token)
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return user_id
