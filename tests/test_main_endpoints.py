import pytest


def test_models_with_pro_token(bmp_client, pro_token):
    res = bmp_client.get(
        "/models", headers={"Authorization": f"Bearer {pro_token}"}
    )
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, dict)
    assert "models" in data
    assert isinstance(data["models"], list)


def test_chat_stub_response(bmp_client, pro_token):
    res = bmp_client.post(
        "/chat", json={"prompt": "Test message"},
        headers={"Authorization": f"Bearer {pro_token}"}
    )
    assert res.status_code == 200
    data = res.json()
    assert data.get("response") == "LLM stub"
