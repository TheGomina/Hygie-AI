import pytest

from backend.bmp_service.normalization import normalize, _NORMALIZATION_MAP


class FakeRepo:
    def __init__(self, mapping):
        self.mapping = mapping

    def find_dci(self, key: str):
        # Return mapping value or None
        return self.mapping.get(key)


@ pytest.fixture(autouse=True)
def fake_repo(monkeypatch):
    # Default repo with no entries
    repo = FakeRepo({})
    monkeypatch.setattr('backend.bmp_service.normalization.get_repository', lambda: repo)
    yield


def test_repo_lookup(monkeypatch):
    # Simulate repo returning a DCI
    mapping = {'MEDX': 'DRUGX'}
    repo = FakeRepo(mapping)
    monkeypatch.setattr('backend.bmp_service.normalization.get_repository', lambda: repo)
    # Even if normalization map has another entry, repo should take precedence
    _NORMALIZATION_MAP['MEDX'] = 'SHOULDBEIGNORED'
    assert normalize('medx') == 'DRUGX'


def test_map_fallback_and_uppercase():
    # Repo returns None, so fallback to map
    # Ensure map entry
    _NORMALIZATION_MAP['ZESTRIL'] = 'LISINOPRIL'
    assert normalize('zestril') == 'LISINOPRIL'
    # Unknown name returns uppercased input
    assert normalize('unknown_name') == 'UNKNOWN_NAME'
