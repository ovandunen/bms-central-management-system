"""E2E-3: Feedback loop — swap recommendation → feedback MQTT → batch export to KI."""

import json
import os
import threading
import time
from datetime import datetime, timezone

import paho.mqtt.client as mqtt
import psycopg2
import pytest
import requests

from conftest import BERLIN_LAT, BERLIN_LON

MQTT_HOST = os.environ.get("MQTT_HOST", "mosquitto-e2e")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
CSMS_HOST = os.environ.get("CSMS_HOST", "solar-csms-app")
CSMS_PORT = os.environ.get("CSMS_PORT", "8080")
VEHICLE_ID = os.environ.get("VEHICLE_ID", "veh-feedback")
POSTGRES_HOST = os.environ.get("POSTGRES_HOST", "postgres-e2e")
POSTGRES_PORT = int(os.environ.get("POSTGRES_PORT", "5432"))
POSTGRES_USER = os.environ.get("POSTGRES_USER", "csms")
POSTGRES_PASSWORD = os.environ.get("POSTGRES_PASSWORD", "csms")
POSTGRES_DB = os.environ.get("POSTGRES_DB", "csmsdb")
WAIT_SECONDS = int(os.environ.get("E2E_SWAP_WAIT_SECONDS", "15"))


def _wait_for_recommendation():
    received = {"payload": None}
    done = threading.Event()

    def on_connect(client, userdata, flags, reason_code, properties=None):
        if reason_code == 0:
            client.subscribe(f"vehicles/{VEHICLE_ID}/swap/recommendation", qos=1)

    def on_message(client, userdata, msg):
        received["payload"] = json.loads(msg.payload.decode("utf-8"))
        done.set()

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="e2e-feedback-tester")
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
    client.loop_start()
    time.sleep(0.5)

    telemetry = {
        "vehicleId": VEHICLE_ID,
        "currentSoc": 18.0,
        "socDelta": -0.5,
        "latitude": BERLIN_LAT,
        "longitude": BERLIN_LON,
        "stationId": "CP-E2E",
        "stationInventory": 2,
        "solarSurplusKw": 40.0,
        "etaMinutes": 12,
    }
    client.publish(
        f"vehicles/{VEHICLE_ID}/telemetry",
        json.dumps(telemetry).encode("utf-8"),
        qos=1,
    ).wait_for_publish(timeout=WAIT_SECONDS)

    assert done.wait(WAIT_SECONDS), "No swap recommendation received"
    client.loop_stop()
    client.disconnect()
    return received["payload"]


def test_feedback_accepted_completed_and_exported_to_ki(seed_swap_e2e_data):
    recommendation = _wait_for_recommendation()
    correlation_id = recommendation["correlationId"]
    assert correlation_id

    feedback_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="e2e-feedback-pub")
    feedback_client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
    feedback_client.loop_start()
    time.sleep(0.3)

    for state, extra in [
        ("accepted", {}),
        ("completed", {
            "stationId": recommendation["stationId"],
            "actualSwapTime": datetime.now(timezone.utc).isoformat(),
        }),
    ]:
        payload = {"vehicleId": VEHICLE_ID, "correlationId": correlation_id, "state": state, **extra}
        feedback_client.publish(
            f"vehicles/{VEHICLE_ID}/swap/feedback",
            json.dumps(payload).encode("utf-8"),
            qos=1,
        ).wait_for_publish(timeout=WAIT_SECONDS)
        time.sleep(0.5)

    feedback_client.loop_stop()
    feedback_client.disconnect()

    connection = psycopg2.connect(
        host=POSTGRES_HOST,
        port=POSTGRES_PORT,
        user=POSTGRES_USER,
        password=POSTGRES_PASSWORD,
        dbname=POSTGRES_DB,
    )
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT COUNT(*) FROM swap_feedback WHERE correlation_id = %s",
                (correlation_id,),
            )
            assert cursor.fetchone()[0] == 2
    finally:
        connection.close()

    export = requests.post(
        f"http://{CSMS_HOST}:{CSMS_PORT}/internal/feedback/export",
        timeout=WAIT_SECONDS,
    )
    assert export.status_code == 200
    assert export.json()["exported"] == 2

    connection = psycopg2.connect(
        host=POSTGRES_HOST,
        port=POSTGRES_PORT,
        user=POSTGRES_USER,
        password=POSTGRES_PASSWORD,
        dbname=POSTGRES_DB,
    )
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT COUNT(*) FROM swap_feedback WHERE correlation_id = %s AND processed = TRUE",
                (correlation_id,),
            )
            assert cursor.fetchone()[0] == 2
    finally:
        connection.close()
