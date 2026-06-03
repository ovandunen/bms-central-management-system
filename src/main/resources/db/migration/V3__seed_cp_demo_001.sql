-- Demo charge point for local OCPP + MQTT integration tests (Berlin centre).
-- BootNotification carries no GPS; this seed ensures PostGIS nearby queries can return CP-DEMO-001.

INSERT INTO charging_station (id, station_id, status, connector_count, last_heartbeat)
VALUES (
    gen_random_uuid(),
    'CP-DEMO-001',
    'AVAILABLE',
    1,
    NOW()
)
ON CONFLICT (station_id) DO UPDATE SET
    status = 'AVAILABLE',
    last_heartbeat = NOW();

INSERT INTO station_location (id, station_id, display_name, solar_capacity_kw, location)
VALUES (
    gen_random_uuid(),
    'CP-DEMO-001',
    'CP-DEMO-001 Demo Solar Hub',
    120.0,
    ST_SetSRID(ST_MakePoint(13.405, 52.52), 4326)
)
ON CONFLICT (station_id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    solar_capacity_kw = EXCLUDED.solar_capacity_kw,
    location = EXCLUDED.location;
