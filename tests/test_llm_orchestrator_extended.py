import pytest
from pathlib import Path
import backend.bmp_service.llm_orchestrator as orch


def test_resolve_name_matching(monkeypatch):
    # Test exact and prefix matching
    monkeypatch.setattr(orch, 'MODEL_PATHS', {
        'alpha': Path('a'),
        'betaModel': Path('b')
    })
    assert orch._resolve_name('alpha') == 'alpha'
    assert orch._resolve_name('Alpha') == 'alpha'
    assert orch._resolve_name('alph') == 'alpha'
    assert orch._resolve_name('betaModel') == 'betaModel'
    assert orch._resolve_name('betamodel') == 'betaModel'
    # Unknown name falls back to first key
    monkeypatch.setattr(orch, 'MODEL_PATHS', {'x': Path('x'), 'y': Path('y')})
    assert orch._resolve_name('unknown') in ['x', 'y']


def test_list_local_models(monkeypatch):
    # Only paths with exists()==True are returned
    class DummyPath:
        def __init__(self, exists_flag):
            self._exists = exists_flag
        def exists(self):
            return self._exists

    mp = {'m1': DummyPath(True), 'm2': DummyPath(False), 'm3': DummyPath(True)}
    monkeypatch.setattr(orch, 'MODEL_PATHS', mp)
    models = orch.list_local_models()
    assert sorted(models) == ['m1', 'm3']
