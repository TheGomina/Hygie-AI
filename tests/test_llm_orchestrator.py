import pytest
import backend.bmp_service.llm_orchestrator as orch


def test_ensemble_majority(monkeypatch):
    # Simulate three models with majority response
    fake_models = {'m1': None, 'm2': None, 'm3': None}
    monkeypatch.setattr(orch, 'MODEL_PATHS', fake_models)
    responses = {'m1': 'X', 'm2': 'Y', 'm3': 'X'}
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, ph, prompt: responses[name])
    monkeypatch.setattr(orch, '_hf_generate', lambda prompt: 'Z')
    result = orch.ensemble_generate('test')
    assert result == 'X'


def test_ensemble_empty(monkeypatch):
    # No local models -> fallback to HF generate
    monkeypatch.setattr(orch, 'MODEL_PATHS', {})
    monkeypatch.setattr(orch, '_hf_generate', lambda prompt: 'HF')
    result = orch.ensemble_generate('test')
    assert result == 'HF'
