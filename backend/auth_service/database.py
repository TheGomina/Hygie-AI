"""Mongo connection helper. Lazy loads to avoid issues in unit tests."""

from pymongo import MongoClient

from .config import COLLECTION_USERS, DB_NAME, MONGO_URL

_client: MongoClient | None = None


def get_client() -> MongoClient:
    global _client
    if _client is None:
        _client = MongoClient(MONGO_URL)
    return _client


def get_collection(name: str):
    return get_client()[DB_NAME][name]


def users_col():
    return get_collection(COLLECTION_USERS)
