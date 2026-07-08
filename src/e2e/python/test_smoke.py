"""E2E smoke tests — stack health only (Story 1.2 telemetry flow deferred)."""

import os
import time

import psycopg2
import pytest
import requests

CSMS_HOST = os.environ.get("CSMS_HOST", "solar-csms-app")
CSMS_PORT = os.environ.get("CSMS_PORT", "8080")
POSTGRES_HOST = os.environ.get("POSTGRES_HOST", "postgres-e2e")
POSTGRES_PORT = int(os.environ.get("POSTGRES_PORT", "5432"))
POSTGRES_USER = os.environ.get("POSTGRES_USER", "csms")
POSTGRES_PASSWORD = os.environ.get("POSTGRES_PASSWORD", "csms")
POSTGRES_DB = os.environ.get("POSTGRES_DB", "csmsdb")
WIREMOCK_KI_URL = os.environ.get("WIREMOCK_KI_URL", "http://wiremock-ki-e2e:8080")
STARTUP_TIMEOUT_SECONDS = int(os.environ.get("E2E_STARTUP_TIMEOUT_SECONDS", "120"))


def _wait_for_http(url: str) -> None:
    deadline = time.time() + STARTUP_TIMEOUT_SECONDS
    last_error = None
    while time.time() < deadline:
        try:
            response = requests.get(url, timeout=5)
            if response.status_code < 500:
                return
            last_error = f"HTTP {response.status_code}"
        except requests.RequestException as exc:
            last_error = str(exc)
        time.sleep(2)
    pytest.fail(f"Timed out waiting for {url}: {last_error}")


def test_csms_app_boots():
    _wait_for_http(f"http://{CSMS_HOST}:{CSMS_PORT}/")


def test_postgres_connects():
    connection = psycopg2.connect(
        host=POSTGRES_HOST,
        port=POSTGRES_PORT,
        user=POSTGRES_USER,
        password=POSTGRES_PASSWORD,
        dbname=POSTGRES_DB,
    )
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
            assert cursor.fetchone()[0] == 1
    finally:
        connection.close()


def test_wiremock_ki_stub_reachable():
    health = requests.get(f"{WIREMOCK_KI_URL}/__admin/health", timeout=10)
    assert health.status_code == 200

    response = requests.post(
        f"{WIREMOCK_KI_URL}/api/optimization/evaluate",
        json={
            "vehicleId": "vehicle-smoke",
            "currentSoc": 18.0,
            "socDelta": -0.5,
            "latitude": 52.52,
            "longitude": 13.405,
            "stationId": "CP-SMOKE",
            "stationInventory": 1,
            "solarSurplusKw": 40.0,
            "etaMinutes": 12,
        },
        timeout=10,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["score"] == pytest.approx(0.92)
    assert body["confidence"] == pytest.approx(0.88)
    assert body["explanation"] == "High solar surplus"
