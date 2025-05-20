import pytest
from pathlib import Path
import httpx
import backend.bmp_service.llm_orchestrator as orch

class DummyPath:
    def __init__(self, exists_flag):
        self._exists = exists_flag
    def exists(self):
        return self._exists


def test_hf_generate_no_key(monkeypatch):
    monkeypatch.setattr(orch, 'HF_API_KEY', '')
    result = orch._hf_generate('prompt')
    assert 'HF_API_KEY absent' in result


def test_hf_generate_api_success(monkeypatch):
    monkeypatch.setattr(orch, 'HF_API_KEY', 'KEY')
    class DummyResp:
        def __init__(self, data):
            self._data = data
        def json(self):
            return self._data
    monkeypatch.setattr(httpx, 'post', lambda url, json, headers, timeout: DummyResp([{'generated_text':'OK'}]))
    result = orch._hf_generate('prompt')
    assert result == 'OK'


def test_ensemble_generate_fallback(monkeypatch):
    monkeypatch.setattr(orch, 'MODEL_PATHS', {})
    monkeypatch.setattr(orch, '_hf_generate', lambda p: 'FB')
    assert orch.ensemble_generate('p') == 'FB'


def test_ensemble_generate_consensus(monkeypatch):
    mp = {
        'biomistral': DummyPath(True),
        'medfound': DummyPath(True),
        'hippomistral': DummyPath(True)
    }
    monkeypatch.setattr(orch, 'MODEL_PATHS', mp)
    seq = {'biomistral':'A','medfound':'B','hippomistral':'B'}
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, hash, prompt: seq[name])
    result = orch.ensemble_generate('p')
    assert result == 'B'


def test_hybrid_generate_summary(monkeypatch):
    calls = []
    responses = ['SUM', 'ANAL', 'REF']
    def fake_ensemble(prompt):
        calls.append(prompt)
        return responses.pop(0)
    monkeypatch.setattr(orch, 'ensemble_generate', fake_ensemble)
    out = orch.hybrid_generate_summary({'age':1}, ['m'], ['pr'])
    assert out == 'REF'
    assert len(calls) == 3


def test_generate_summary_fallback(monkeypatch):
    monkeypatch.setattr(orch, 'hybrid_generate_summary', lambda *args, **kwargs: (_ for _ in ()).throw(Exception('oops')))
    monkeypatch.setattr(orch, '_resolve_name', lambda name: 'X')
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, hash, prompt: 'FALL')
    res = orch.generate_summary({}, [], [], model_name='any')
    assert res == 'FALL'


def test_generate_summary_hybrid_default(monkeypatch):
    import backend.bmp_service.llm_orchestrator as orch
    # Hybrid strategy by default
    monkeypatch.setattr(orch, 'hybrid_generate_summary', lambda demo, meds, probs: 'HY')
    res = orch.generate_summary({'age':1}, ['m'], ['p'], model_name='any')
    assert res == 'HY'


def test_generate_summary_ensemble(monkeypatch):
    import backend.bmp_service.llm_orchestrator as orch
    # Ensemble strategy when specified
    monkeypatch.setattr(orch, 'ensemble_generate', lambda prompt: 'EN')
    res = orch.generate_summary({'age':1}, ['m'], ['p'], model_name='ensemble')
    assert res == 'EN'


def test_generate_summary_cascade(monkeypatch):
    import backend.bmp_service.llm_orchestrator as orch
    # Cascade strategy when specified
    monkeypatch.setattr(orch, 'cascade_generate', lambda prompt: 'CA')
    res = orch.generate_summary({'age':1}, ['m'], ['p'], model_name='cascade')
    assert res == 'CA'
