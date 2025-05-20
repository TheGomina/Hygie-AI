import pytest
import backend.auth_service.database as dbmod

# Dummy classes to simulate MongoClient, DB, and Collection
class DummyCollection:
    def __init__(self, name):
        self.name = name

class DummyDB:
    def __init__(self, name):
        self.name = name
        self.collections = {}
    def __getitem__(self, coll_name):
        if coll_name not in self.collections:
            self.collections[coll_name] = DummyCollection(coll_name)
        return self.collections[coll_name]

class DummyMongoClient:
    def __init__(self, url):
        self.url = url
        self.databases = {}
    def __getitem__(self, db_name):
        if db_name not in self.databases:
            self.databases[db_name] = DummyDB(db_name)
        return self.databases[db_name]


def test_get_client_caching(monkeypatch):
    # Setup dummy MongoClient and URL
    dummy_url = "mongodb://testdb:27017"
    monkeypatch.setattr(dbmod, "MONGO_URL", dummy_url)
    monkeypatch.setattr(dbmod, "_client", None)
    monkeypatch.setattr(dbmod, "MongoClient", DummyMongoClient)

    client1 = dbmod.get_client()
    assert isinstance(client1, DummyMongoClient)
    assert client1.url == dummy_url

    client2 = dbmod.get_client()
    # Caching: same instance returned
    assert client2 is client1


def test_get_collection_and_users_col(monkeypatch):
    # Setup dummy MongoClient
    monkeypatch.setattr(dbmod, "_client", None)
    monkeypatch.setattr(dbmod, "MongoClient", DummyMongoClient)
    # Override DB_NAME and COLLECTION_USERS
    monkeypatch.setattr(dbmod, "DB_NAME", "testdb")
    monkeypatch.setattr(dbmod, "COLLECTION_USERS", "theusers")

    # get_collection
    coll = dbmod.get_collection("coll1")
    assert isinstance(coll, DummyCollection)
    assert coll.name == "coll1"

    # users_col uses COLLECTION_USERS
    uc = dbmod.users_col()
    assert isinstance(uc, DummyCollection)
    assert uc.name == "theusers"
