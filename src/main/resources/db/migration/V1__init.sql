CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE charging_station (
    id UUID PRIMARY KEY,
    station_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    last_heartbeat TIMESTAMPTZ,
    connector_count INT NOT NULL DEFAULT 1
);

CREATE TABLE station_location (
    id UUID PRIMARY KEY,
    station_id VARCHAR(64) NOT NULL UNIQUE REFERENCES charging_station(station_id),
    display_name VARCHAR(255),
    solar_capacity_kw DOUBLE PRECISION,
    location GEOMETRY(Point, 4326) NOT NULL
);

CREATE INDEX idx_station_location_geo ON station_location USING GIST (location);
