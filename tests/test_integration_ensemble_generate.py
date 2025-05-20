import pytest
from backend.bmp_service.llm_orchestrator import MODEL_PATHS, ensemble_generate

@pytest.mark.integration
@pytest.mark.skipif(
    not MODEL_PATHS,
    reason="No local LLM models found in settings.llm_base_dir"
)
def test_ensemble_generate_e2e():
    """
    End-to-end integration test for ensemble_generate pipeline.
    Skips if no local models.
    """
    prompt = "Test pipeline"
    try:
        result = ensemble_generate(prompt)
    except ImportError as exc:
        pytest.skip(f"Skipping integration ensemble: {exc}")
    assert isinstance(result, str) and result.strip(), "Ensemble generate returned empty"
