import pytest
from backend.bmp_service import llm_orchestrator as orch
from pathlib import Path

class DummyPath:
    def exists(self):
        return True

class DummyHist:
    def __init__(self):
        self.called = []
    def labels(self, **kwargs):
        self.called.append(kwargs)
        return self
    def time(self):
        class DummyCM:
            def __enter__(self):
                pass
            def __exit__(self, exc_type, exc, tb):
                pass
        return DummyCM()


def test_track_decorator_ensemble(monkeypatch):
    dummy = DummyHist()
    monkeypatch.setattr(orch, 'LLM_ORCH_HISTOGRAM', dummy)
    # override MODEL_PATHS and generators
    monkeypatch.setattr(orch, 'MODEL_PATHS', {
        'biomistral': DummyPath(),
        'medfound': DummyPath(),
        'hippomistral': DummyPath(),
    })
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, h, p: 'res')
    res = orch.ensemble_generate('p')
    assert res == 'res'
    assert {'stage': 'ensemble_generate'} in dummy.called


def test_track_decorator_hybrid(monkeypatch):
    dummy = DummyHist()
    monkeypatch.setattr(orch, 'LLM_ORCH_HISTOGRAM', dummy)
    # stub ensemble_generate
    monkeypatch.setattr(orch, 'ensemble_generate', lambda prompt: 'out')
    demo = {'a': 1}
    meds = ['m']
    probs = ['p']
    res = orch.hybrid_generate_summary(demo, meds, probs)
    assert res == 'out'
    assert {'stage': 'hybrid_generate_summary'} in dummy.called


def test_ensemble_fallback_to_hf_on_error(monkeypatch):
    monkeypatch.setattr(orch, 'MODEL_PATHS', {'m1': None})
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, h, p: (_ for _ in ()).throw(Exception()))
    monkeypatch.setattr(orch, '_hf_generate', lambda p: 'hf')
    res = orch.ensemble_generate('p')
    assert res == 'hf'


def test_generate_summary_fallback(monkeypatch):
    # hybrid raises error → fallback to local
    monkeypatch.setattr(orch, 'hybrid_generate_summary', lambda a, b, c: (_ for _ in ()).throw(Exception()))
    monkeypatch.setattr(orch, '_resolve_name', lambda name: 'xx')
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, h, p: 'localres')
    monkeypatch.setattr(orch, '_hf_generate', lambda p: 'hf')
    demo = {'x': 0}
    res = orch.generate_summary(demo, ['m'], ['p'], model_name='abc')
    assert res == 'localres'

    # local also fails → fallback to HF
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, h, p: (_ for _ in ()).throw(Exception()))
    res2 = orch.generate_summary(demo, ['m'], ['p'], model_name='abc')
    assert res2 == 'hf'


def test_metrics_endpoint_content(bmp_client):
    res = bmp_client.get('/metrics')
    assert res.status_code == 200
    assert res.headers['content-type'] == 'text/plain; version=0.0.4; charset=utf-8'
    assert res.content == b''
