"""E2E smoke tests — stack health and KI stub routing only."""

import requests


def test_csms_app_is_healthy():
    # Quarkus is up if it returns a standard HTTP error (e.g., 404) rather than a connection refusal
    response = requests.get("http://solar-csms-app:8080/", timeout=10)
    assert response.status_code < 500


def test_ki_stub_is_reachable():
    response = requests.post(
        "http://wiremock-ki-e2e:8080/api/optimization/evaluate",
        json={"vehicleId": "smoke"},
        timeout=10,
    )
    assert response.status_code == 200
    assert response.json()["score"] == 0.95
