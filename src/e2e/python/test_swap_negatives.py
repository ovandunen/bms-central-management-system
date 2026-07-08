"""E2E-2 negative scenarios: cooldown, KI fallback, low confidence suppression."""

import json
import os
import threading
import time

import paho.mqtt.client as mqtt
import pytest
import requests

from conftest import BERLIN_LAT, BERLIN_LON

MQTT_HOST = os.environ.get("MQTT_HOST", "mosquitto-e2e")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
VEHICLE_ID = os.environ.get("VEHICLE_ID", "veh-neg")
WIREMOCK_KI_URL = os.environ.get("WIREMOCK_KI_URL", "http://wiremock-ki-e2e:8080")
WAIT_SECONDS = int(os.environ.get("E2E_SWAP_WAIT_SECONDS", "15"))
COOLDOWN_SECONDS = int(os.environ.get("E2E_COOLDOWN_SECONDS", "5"))


def _listen_for_recommendations(max_messages: int, vehicle_id: str):
    messages = []
    done = threading.Event()

    def on_connect(client, userdata, flags, reason_code, properties=None):
        if reason_code == 0:
            client.subscribe(f"vehicles/{vehicle_id}/swap/recommendation", qos=1)

    def on_message(client, userdata, msg):
        messages.append(json.loads(msg.payload.decode("utf-8")))
        if len(messages) >= max_messages:
            done.set()

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"e2e-neg-{vehicle_id}")
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
    client.loop_start()
    time.sleep(0.5)
    return client, messages, done


def _publish_telemetry(client, vehicle_id: str, soc: float = 18.0):
    telemetry = {
        "vehicleId": vehicle_id,
        "currentSoc": soc,
        "socDelta": -0.5,
        "latitude": BERLIN_LAT,
        "longitude": BERLIN_LON,
        "stationId": "CP-E2E",
        "stationInventory": 2,
        "solarSurplusKw": 40.0,
        "etaMinutes": 12,
    }
    client.publish(
        f"vehicles/{vehicle_id}/telemetry",
        json.dumps(telemetry).encode("utf-8"),
        qos=1,
    ).wait_for_publish(timeout=WAIT_SECONDS)



def test_cooldown_suppresses_second_recommendation(seed_swap_e2e_data):
    vehicle_id = "veh-cooldown"
    client, messages, done = _listen_for_recommendations(2, vehicle_id)

    _publish_telemetry(client, vehicle_id)
    time.sleep(2)
    _publish_telemetry(client, vehicle_id)
    time.sleep(2)

    client.loop_stop()
    client.disconnect()
    assert len(messages) == 1, f"Expected 1 recommendation during cooldown, got {len(messages)}"


def test_low_ki_confidence_suppresses_recommendation(seed_swap_e2e_data, reset_wiremock_ki_stub):
    vehicle_id = "veh-lowconf"
    requests.post(
        f"{WIREMOCK_KI_URL}/__admin/mappings",
        json={
            "priority": 1,
            "request": {"method": "POST", "urlPath": "/api/optimization/evaluate"},
            "response": {
                "status": 200,
                "headers": {"Content-Type": "application/json"},
                "jsonBody": {
                    "score": 0.92,
                    "validFrom": "2030-06-25T14:00:00Z",
                    "validUntil": "2030-06-25T14:30:00Z",
                    "confidence": 0.55,
                    "explanation": "Low confidence",
                },
            },
        },
        timeout=10,
    )

    client, messages, done = _listen_for_recommendations(1, vehicle_id)
    _publish_telemetry(client, vehicle_id)
    time.sleep(WAIT_SECONDS)

    client.loop_stop()
    client.disconnect()
    assert len(messages) == 0, "Low-confidence KI response should suppress recommendation"


def test_ki_unavailable_falls_back_to_rule_based_recommendation(seed_swap_e2e_data, reset_wiremock_ki_stub):
    vehicle_id = "veh-fallback"
    requests.post(
        f"{WIREMOCK_KI_URL}/__admin/mappings",
        json={
            "priority": 1,
            "request": {"method": "POST", "urlPath": "/api/optimization/evaluate"},
            "response": {"status": 500, "jsonBody": {"error": "down"}},
        },
        timeout=10,
    )

    client, messages, done = _listen_for_recommendations(1, vehicle_id)
    _publish_telemetry(client, vehicle_id)
    assert done.wait(WAIT_SECONDS), "Expected rule-based fallback recommendation"

    client.loop_stop()
    client.disconnect()
    assert messages[0]["stationId"]
    assert messages[0]["confidence"] > 0
    # Allow circuit breaker to close before subsequent tests reuse the KI client.
    time.sleep(6)
