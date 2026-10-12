"""Cross-cutting: malformed MQTT payloads must not crash CSMS (Story 5.3 / CR-1)."""

import json
import os
import time

import paho.mqtt.client as mqtt
import pytest
import requests

MQTT_HOST = os.environ.get("MQTT_HOST", "mosquitto-e2e")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
CSMS_HOST = os.environ.get("CSMS_HOST", "solar-csms-app")
CSMS_PORT = os.environ.get("CSMS_PORT", "8080")


def _publish_raw(topic: str, payload: bytes):
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"e2e-malformed-{topic[-8:]}")
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
    client.loop_start()
    time.sleep(0.2)
    client.publish(topic, payload, qos=1).wait_for_publish(timeout=10)
    client.loop_stop()
    client.disconnect()


@pytest.mark.parametrize(
    "topic,payload",
    [
        ("vehicles/EV-001/telemetry", b"{not-json"),
        ("drivers/EV-001/request/location", b"[]"),
        ("vehicles/EV-001/swap/feedback", b'{"state":"accepted"}'),
    ],
)
def test_malformed_mqtt_does_not_crash_csms(topic, payload):
    _publish_raw(topic, payload)
    time.sleep(1)
    response = requests.get(f"http://{CSMS_HOST}:{CSMS_PORT}/", timeout=10)
    assert response.status_code < 500
