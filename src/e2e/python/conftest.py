"""Shared E2E fixtures: database seed data for swap station scenarios."""

import os
import uuid

import psycopg2
import pytest
import requests

POSTGRES_HOST = os.environ.get("POSTGRES_HOST", "postgres-e2e")
POSTGRES_PORT = int(os.environ.get("POSTGRES_PORT", "5432"))
POSTGRES_USER = os.environ.get("POSTGRES_USER", "csms")
POSTGRES_PASSWORD = os.environ.get("POSTGRES_PASSWORD", "csms")
POSTGRES_DB = os.environ.get("POSTGRES_DB", "csmsdb")
WIREMOCK_KI_URL = os.environ.get("WIREMOCK_KI_URL", "http://wiremock-ki-e2e:8080")

# Berlin center — matches pull/swap test coordinates
BERLIN_LAT = 52.52
BERLIN_LON = 13.405


@pytest.fixture(autouse=True)
def reset_wiremock_ki_stub():
    """Restore file-based WireMock mappings between tests to avoid cross-test pollution."""
    requests.post(f"{WIREMOCK_KI_URL}/__admin/mappings/reset", timeout=10)
    yield
    requests.post(f"{WIREMOCK_KI_URL}/__admin/mappings/reset", timeout=10)


def _connect():
    return psycopg2.connect(
        host=POSTGRES_HOST,
        port=POSTGRES_PORT,
        user=POSTGRES_USER,
        password=POSTGRES_PASSWORD,
        dbname=POSTGRES_DB,
    )


def _clear_swap_data(cursor):
    cursor.execute("DELETE FROM swap_feedback")
    cursor.execute("DELETE FROM swap_recommendation")
    cursor.execute("DELETE FROM station_swap_state")
    cursor.execute("DELETE FROM station_location")
    cursor.execute("DELETE FROM charging_station")


def _insert_station(cursor, station_id, lat, lon, name, packs, bay_free, solar_surplus, telemetry_age_minutes):
    station_uuid = str(uuid.uuid4())
    location_uuid = str(uuid.uuid4())
    cursor.execute(
        """
        INSERT INTO charging_station (id, station_id, status, last_heartbeat, connector_count)
        VALUES (%s, %s, 'AVAILABLE', NOW(), 2)
        """,
        (station_uuid, station_id),
    )
    cursor.execute(
        """
        INSERT INTO station_location (id, station_id, display_name, solar_capacity_kw, location)
        VALUES (%s, %s, %s, 120.0, ST_SetSRID(ST_MakePoint(%s, %s), 4326))
        """,
        (location_uuid, station_id, name, lon, lat),
    )
    cursor.execute(
        """
        INSERT INTO station_swap_state
            (station_id, charged_packs_ready, swap_bay_free, solar_surplus_kw, telemetry_updated_at)
        VALUES (%s, %s, %s, %s, NOW() - (%s || ' minutes')::interval)
        """,
        (station_id, packs, bay_free, solar_surplus, telemetry_age_minutes),
    )


@pytest.fixture()
def seed_swap_e2e_data():
    """Berlin swap station (CP-E2E) with clean feedback/recommendation tables for swap MQTT tests."""
    connection = _connect()
    try:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM swap_feedback")
            cursor.execute("DELETE FROM swap_recommendation")
            _clear_swap_data(cursor)
            _insert_station(
                cursor, "CP-E2E", BERLIN_LAT, BERLIN_LON, "E2E Swap Station", 2, True, 40.0, 1
            )
        connection.commit()
        yield
    finally:
        connection.close()


@pytest.fixture()
def seed_swap_stations():
    """Five stations with mixed swap eligibility (E2E-1 test data)."""
    connection = _connect()
    try:
        with connection.cursor() as cursor:
            _clear_swap_data(cursor)
            _insert_station(cursor, "Station-A", BERLIN_LAT + 0.01, BERLIN_LON + 0.01, "Station A", 3, True, 45.0, 1)
            _insert_station(cursor, "Station-B", BERLIN_LAT + 0.02, BERLIN_LON + 0.02, "Station B", 2, True, 30.0, 2)
            _insert_station(cursor, "Station-C", BERLIN_LAT + 0.03, BERLIN_LON + 0.03, "Station C", 0, True, 10.0, 1)
            _insert_station(cursor, "Station-D", BERLIN_LAT + 0.04, BERLIN_LON + 0.04, "Station D", 2, False, 10.0, 1)
            _insert_station(cursor, "Station-E", BERLIN_LAT + 0.05, BERLIN_LON + 0.05, "Station E", 4, True, 50.0, 10)
        connection.commit()
        yield
        with connection.cursor() as cursor:
            _clear_swap_data(cursor)
        connection.commit()
    finally:
        connection.close()


@pytest.fixture()
def seed_no_eligible_stations():
    """Only ineligible stations within range."""
    connection = _connect()
    try:
        with connection.cursor() as cursor:
            _clear_swap_data(cursor)
            _insert_station(cursor, "Station-C", BERLIN_LAT + 0.01, BERLIN_LON + 0.01, "No packs", 0, True, 0, 1)
            _insert_station(cursor, "Station-D", BERLIN_LAT + 0.02, BERLIN_LON + 0.02, "Bay busy", 2, False, 0, 1)
        connection.commit()
        yield
        with connection.cursor() as cursor:
            _clear_swap_data(cursor)
        connection.commit()
    finally:
        connection.close()
