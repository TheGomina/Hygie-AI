from datetime import datetime, timezone
from typing import Any, Dict

from jose import jwt
from passlib.context import CryptContext

from .config import ACCESS_TOKEN_EXPIRE, ALGORITHM, SECRET_KEY

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


def hash_password(pwd: str) -> str:
    return pwd_context.hash(pwd)


def create_access_token(subject: str) -> str:
    expire = datetime.now(timezone.utc) + ACCESS_TOKEN_EXPIRE
    to_encode: Dict[str, Any] = {"sub": subject, "exp": expire}
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
