import pytest
import pytest
from backend.bmp_service import llm_orchestrator as orch

# Dummy path for MODEL_PATHS entries
class DummyPath:
    def exists(self):
        return True

@pytest.fixture(autouse=True)
def dummy_model_paths(monkeypatch):
    # Override MODEL_PATHS to include three dummy models
    monkeypatch.setattr(orch, "MODEL_PATHS", {
        "biomistral": DummyPath(),
        "medfound": DummyPath(),
        "hippomistral": DummyPath(),
    })
    yield


def test_ensemble_consensus(monkeypatch):
    # Two models return same, one different → consensus picks the majority
    monkeypatch.setattr(orch, "_cached_local_generate", lambda name, h, p: "res" if name != "hippomistral" else "other")
    monkeypatch.setattr(orch, "_hf_generate", lambda p: "hf")
    result = orch.ensemble_generate("prompt")
    assert result == "res"


def test_ensemble_fallback_to_hf(monkeypatch):
    # No local models available → fallback to HF
    monkeypatch.setattr(orch, "MODEL_PATHS", {})
    monkeypatch.setattr(orch, "_hf_generate", lambda p: "hf")
    result = orch.ensemble_generate("p")
    assert result == "hf"


def test_hybrid_generate_summary(monkeypatch):
    # Stub ensemble_generate to simulate pipeline stages
    calls = []
    def fake_ensemble(prompt):
        calls.append(prompt)
        if prompt.startswith("Vous êtes un pharmacien"):
            return "synth"
        if prompt.startswith("Analyse factuelle"):
            return "analysis"
        if prompt.startswith("Reformule"):
            return "final"
        return ""
    monkeypatch.setattr(orch, "ensemble_generate", fake_ensemble)
    demo = {"age": 50, "sex": "F"}
    meds = ["med1", "med2"]
    probs = ["prob1"]
    summary = orch.hybrid_generate_summary(demo, meds, probs)
    assert summary == "final"
    assert len(calls) == 3
    assert calls[0].startswith("Vous êtes un pharmacien")
    assert calls[1].startswith("Analyse factuelle")
    assert calls[2].startswith("Reformule")


def test_cascade_generate_sequence(monkeypatch):
    import backend.bmp_service.llm_orchestrator as orch
    # Setup dummy model paths
    from tests.test_llm_orchestration import DummyPath
    mp = {
        'biomistral': DummyPath(),
        'medfound': DummyPath(),
        'hippomistral': DummyPath(),
    }
    monkeypatch.setattr(orch, 'MODEL_PATHS', mp)
    # Define outputs for each stage
    seq = {'biomistral': 'SUM', 'medfound': 'ANAL', 'hippomistral': 'FINAL'}
    monkeypatch.setattr(orch, '_cached_local_generate', lambda name, h, p: seq[name])
    # Should return final stage output
    result = orch.cascade_generate('prompt')
    assert result == 'FINAL'


def test_cascade_generate_fallback(monkeypatch):
    import backend.bmp_service.llm_orchestrator as orch
    # No local models available
    monkeypatch.setattr(orch, 'MODEL_PATHS', {})
    monkeypatch.setattr(orch, '_hf_generate', lambda p: 'HFALL')
    # All stages fallback to HF
    result = orch.cascade_generate('prompt')
    assert result == 'HFALL'
