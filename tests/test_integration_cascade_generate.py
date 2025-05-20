import pytest
from backend.bmp_service.llm_orchestrator import MODEL_PATHS, cascade_generate

@pytest.mark.integration
@pytest.mark.skipif(
    not MODEL_PATHS,
    reason="No local LLM models found in settings.llm_base_dir"
)
def test_cascade_generate_e2e():
    """
    End-to-end integration test for cascade_generate pipeline.
    Skips if no local models.
    """
    prompt = "Test pipeline"
    try:
        result = cascade_generate(prompt)
    except ImportError as exc:
        pytest.skip(f"Skipping integration cascade: {exc}")
    assert isinstance(result, str) and result.strip(), "Cascade generate returned empty"
