import pytest
pytest.importorskip("torch", reason="Torch unavailable or broken", exc_type=ImportError)
from backend.bmp_service.llm_orchestrator import MODEL_PATHS, _load_model

@pytest.mark.integration
@pytest.mark.skipif(
    not MODEL_PATHS,
    reason="No local LLM models found in settings.llm_base_dir"
)
def test_load_local_llm_models():
    """
    Test that each local LLM model defined in MODEL_PATHS can be loaded without errors.
    Skips if no models are present.
    """
    for name in MODEL_PATHS:
        try:
            tokenizer, model = _load_model(name)
        except ImportError as exc:
            pytest.skip(f"Skipping integration test: {exc}")
        # Basic sanity checks
        assert tokenizer is not None
        assert hasattr(tokenizer, 'encode'), f"Tokenizer for {name} lacks encode"
        assert model is not None
        assert hasattr(model, 'generate'), f"Model for {name} lacks generate"
