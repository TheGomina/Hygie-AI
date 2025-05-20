from fastapi.testclient import TestClient
from backend.bmp_service.main import app

client = TestClient(app)

def test_metrics_endpoint():
    # Ensure metrics endpoint returns Prometheus data
    response = client.get("/metrics")
    assert response.status_code == 200
    ct = response.headers.get("content-type", "")
    assert ct.startswith("text/plain")
    text = response.text
    # Check for HELP and TYPE lines for the request counter
    assert "# HELP bmp_requests_total Total HTTP requests (BMP)" in text
    assert "# TYPE bmp_requests_total counter" in text
    # At least one metric sample present
    assert "bmp_requests_total_total" in text or "bmp_request_processing_seconds" in text
