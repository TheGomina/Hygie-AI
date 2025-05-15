"""Configuration and constants for the Auth Service.
Environment variables override the defaults so that the same
container image can be reused across environments.
"""

import os
from datetime import timedelta

# Secret used for JWT signing – MUST be overridden in prod (e.g. in a Kubernetes Secret)
SECRET_KEY: str = os.getenv("AUTH_SECRET_KEY", "CHANGE_ME_IN_PROD")

# HS256 → compatible with most clients, fast & compact
ALGORITHM: str = "HS256"

# Access-token lifespan (minutes)
ACCESS_TOKEN_EXPIRE_MINUTES: int = int(os.getenv("ACCESS_TOKEN_EXPIRE", "30"))
ACCESS_TOKEN_EXPIRE: timedelta = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)

# Mongo connection – bare-minimum parameters
MONGO_URL: str = os.getenv("MONGO_URL", "mongodb://localhost:27017")
DB_NAME: str = os.getenv("MONGO_DB", "hygie")
COLLECTION_USERS: str = "users"
