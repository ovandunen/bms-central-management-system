"""E2E swap flow: telemetry in → KI stub → swap recommendation out (Stories 1.2, 2.1–2.3)."""

import json
import os
import threading
import time

import paho.mqtt.client as mqtt
import pytest

MQTT_HOST = os.environ.get("MQTT_HOST", "mosquitto-e2e")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
VEHICLE_ID = os.environ.get("VEHICLE_ID", "veh-001")
WAIT_SECONDS = int(os.environ.get("E2E_SWAP_WAIT_SECONDS", "15"))


@pytest.fixture()
def mqtt_recommendation_listener():
    received = {"payload": None}
    ready = threading.Event()
    done = threading.Event()

    def on_connect(client, userdata, flags, reason_code, properties=None):
        if reason_code == 0:
            client.subscribe(f"vehicles/{VEHICLE_ID}/swap/recommendation", qos=1)

    def on_subscribe(client, userdata, mid, reason_code_list, properties=None):
        ready.set()

    def on_message(client, userdata, msg):
        received["payload"] = json.loads(msg.payload.decode("utf-8"))
        done.set()

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="e2e-swap-tester")
    client.on_connect = on_connect
    client.on_subscribe = on_subscribe
    client.on_message = on_message
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
    client.loop_start()

    assert ready.wait(WAIT_SECONDS), f"MQTT subscribe did not complete ({MQTT_HOST}:{MQTT_PORT})"
    time.sleep(0.5)

    yield received, done, client

    client.loop_stop()
    client.disconnect()


def test_swap_recommendation_after_vehicle_telemetry(seed_swap_e2e_data, mqtt_recommendation_listener):
    received, done, client = mqtt_recommendation_listener

    telemetry = {
        "vehicleId": VEHICLE_ID,
        "currentSoc": 18.0,
        "socDelta": -0.5,
        "latitude": 52.52,
        "longitude": 13.405,
        "stationId": "CP-E2E",
        "stationInventory": 2,
        "solarSurplusKw": 40.0,
        "etaMinutes": 12,
    }
    publish = client.publish(
        f"vehicles/{VEHICLE_ID}/telemetry",
        json.dumps(telemetry).encode("utf-8"),
        qos=1,
    )
    publish.wait_for_publish(timeout=WAIT_SECONDS)

    assert done.wait(WAIT_SECONDS), (
        f"No message on vehicles/{VEHICLE_ID}/swap/recommendation within {WAIT_SECONDS}s"
    )

    body = received["payload"]
    assert body.get("correlationId")
    assert body.get("stationId") is not None
    assert body.get("confidence") is not None
    assert body.get("validFrom") is not None
    assert body.get("validUntil") is not None
    assert body.get("reason") is not None
    assert body.get("routeEta") is not None
    assert body.get("latitude") is not None
    assert body.get("longitude") is not None
