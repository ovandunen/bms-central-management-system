-- PLACEHOLDER WGS-84 coordinates along the Zürich–Dakar corridor.
-- Commissioning must replace these with surveyed GPS before production deployment.
-- Station IDs follow fleet conventions: location+unit (ZUR-DEPOT-01), sequential (CP-000n), site (DKR-STATION-03).

INSERT INTO charging_station (id, station_id, status, connector_count)
VALUES
    ('a0000001-0000-4000-8000-000000000001', 'ZUR-DEPOT-01', 'AVAILABLE', 2),
    ('a0000001-0000-4000-8000-000000000002', 'CP-0001', 'AVAILABLE', 2),
    ('a0000001-0000-4000-8000-000000000003', 'CP-0002', 'AVAILABLE', 2),
    ('a0000001-0000-4000-8000-000000000004', 'DKR-STATION-03', 'AVAILABLE', 2),
    ('a0000001-0000-4000-8000-000000000005', 'CP-SWAP-01', 'AVAILABLE', 2),
    ('a0000001-0000-4000-8000-000000000006', 'CP-E2E', 'AVAILABLE', 2)
ON CONFLICT (station_id) DO NOTHING;

INSERT INTO station_location (id, station_id, display_name, solar_capacity_kw, location, city, street_address)
VALUES
    (
        'b0000001-0000-4000-8000-000000000001',
        'ZUR-DEPOT-01',
        'Zürich Depot 01 (PLACEHOLDER)',
        120.0,
        ST_SetSRID(ST_MakePoint(8.5417, 47.3769), 4326),
        'Zürich',
        'PLACEHOLDER — replace before production'
    ),
    (
        'b0000001-0000-4000-8000-000000000002',
        'CP-0001',
        'Corridor CP-0001 (PLACEHOLDER)',
        100.0,
        ST_SetSRID(ST_MakePoint(6.6323, 46.5197), 4326),
        'Lausanne corridor',
        'PLACEHOLDER — replace before production'
    ),
    (
        'b0000001-0000-4000-8000-000000000003',
        'CP-0002',
        'Corridor CP-0002 (PLACEHOLDER)',
        100.0,
        ST_SetSRID(ST_MakePoint(1.4442, 43.6047), 4326),
        'Toulouse corridor',
        'PLACEHOLDER — replace before production'
    ),
    (
        'b0000001-0000-4000-8000-000000000004',
        'DKR-STATION-03',
        'Dakar Station 03 (PLACEHOLDER)',
        150.0,
        ST_SetSRID(ST_MakePoint(-17.4677, 14.7167), 4326),
        'Dakar',
        'PLACEHOLDER — replace before production'
    ),
    (
        'b0000001-0000-4000-8000-000000000005',
        'CP-SWAP-01',
        'Swap test hub (PLACEHOLDER)',
        80.0,
        ST_SetSRID(ST_MakePoint(13.405, 52.52), 4326),
        'Berlin',
        'PLACEHOLDER — replace before production'
    ),
    (
        'b0000001-0000-4000-8000-000000000006',
        'CP-E2E',
        'E2E swap test station (PLACEHOLDER)',
        100.0,
        ST_SetSRID(ST_MakePoint(13.405, 52.52), 4326),
        'Berlin',
        'PLACEHOLDER — E2E / dev only'
    )
ON CONFLICT (station_id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    solar_capacity_kw = EXCLUDED.solar_capacity_kw,
    location = EXCLUDED.location,
    city = EXCLUDED.city,
    street_address = EXCLUDED.street_address;

INSERT INTO station_swap_state (station_id, charged_packs_ready, swap_bay_free, solar_surplus_kw)
VALUES
    ('ZUR-DEPOT-01', 3, TRUE, 12.0),
    ('CP-0001', 2, TRUE, 8.0),
    ('CP-0002', 2, TRUE, 6.0),
    ('DKR-STATION-03', 4, TRUE, 20.0),
    ('CP-SWAP-01', 2, TRUE, 5.0),
    ('CP-E2E', 2, TRUE, 40.0)
ON CONFLICT (station_id) DO UPDATE SET
    charged_packs_ready = EXCLUDED.charged_packs_ready,
    swap_bay_free = EXCLUDED.swap_bay_free,
    solar_surplus_kw = EXCLUDED.solar_surplus_kw;
