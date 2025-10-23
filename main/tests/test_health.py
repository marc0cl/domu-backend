"""Integration tests for the health endpoint."""

from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import create_app


def test_health_endpoint_returns_success_payload() -> None:
    client = TestClient(create_app())

    response = client.get("/api/health/")

    assert response.status_code == 200
    payload = response.json()
    assert payload["service"] == "Domu Backend"
    assert payload["status"] == "healthy"
    assert payload["environment"] == "development"
    assert "timestamp" in payload
