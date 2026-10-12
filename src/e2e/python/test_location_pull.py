"""E2E-1: Low SoC pull flow — drivers/{id}/request/location → stations/available/{id}."""

import json
import os
import threading
import time

import paho.mqtt.client as mqtt
import pytest

from conftest import BERLIN_LAT, BERLIN_LON

MQTT_HOST = os.environ.get("MQTT_HOST", "mosquitto-e2e")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
DRIVER_ID = os.environ.get("DRIVER_ID", "EV-001")
WAIT_SECONDS = int(os.environ.get("E2E_PULL_WAIT_SECONDS", "15"))


@pytest.fixture()
def mqtt_station_listener():
    received = {"payload": None}
    ready = threading.Event()
    done = threading.Event()

    def on_connect(client, userdata, flags, reason_code, properties=None):
        if reason_code == 0:
            client.subscribe(f"stations/available/{DRIVER_ID}", qos=1)

    def on_subscribe(client, userdata, mid, reason_code_list, properties=None):
        ready.set()

    def on_message(client, userdata, msg):
        received["payload"] = json.loads(msg.payload.decode("utf-8"))
        done.set()

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="e2e-pull-tester")
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


def test_pull_returns_eligible_swap_stations_with_correlation(seed_swap_stations, mqtt_station_listener):
    received, done, client = mqtt_station_listener

    request = {
        "driverId": DRIVER_ID,
        "latitude": BERLIN_LAT,
        "longitude": BERLIN_LON,
        "radiusMeters": 50_000,
    }
    publish = client.publish(
        f"drivers/{DRIVER_ID}/request/location",
        json.dumps(request).encode("utf-8"),
        qos=1,
    )
    publish.wait_for_publish(timeout=WAIT_SECONDS)

    assert done.wait(WAIT_SECONDS), f"No response on stations/available/{DRIVER_ID}"

    body = received["payload"]
    assert body["schemaVersion"] == 1
    assert body["correlationId"]
    station_ids = {s["stationId"] for s in body["stations"]}
    assert "Station-A" in station_ids
    assert "Station-B" in station_ids
    assert "Station-C" not in station_ids
    assert "Station-D" not in station_ids
    assert "Station-E" not in station_ids

    station_a = next(s for s in body["stations"] if s["stationId"] == "Station-A")
    assert station_a["chargedPacksReady"] == 3
    assert station_a["swapBayFree"] is True
    assert station_a["solarSurplusKw"] == pytest.approx(45.0)
    assert station_a["distanceKm"] is not None


def test_pull_returns_empty_when_no_eligible_stations(seed_no_eligible_stations, mqtt_station_listener):
    received, done, client = mqtt_station_listener

    request = {
        "driverId": DRIVER_ID,
        "latitude": BERLIN_LAT,
        "longitude": BERLIN_LON,
        "radiusMeters": 50_000,
    }
    client.publish(
        f"drivers/{DRIVER_ID}/request/location",
        json.dumps(request).encode("utf-8"),
        qos=1,
    ).wait_for_publish(timeout=WAIT_SECONDS)

    assert done.wait(WAIT_SECONDS)
    body = received["payload"]
    assert body["stations"] == []
    assert "No swap stations available" in body["message"]
