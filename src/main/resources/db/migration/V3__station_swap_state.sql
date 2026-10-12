CREATE TABLE station_swap_state (
    station_id VARCHAR(64) PRIMARY KEY REFERENCES charging_station(station_id),
    charged_packs_ready INT NOT NULL DEFAULT 0,
    swap_bay_free BOOLEAN NOT NULL DEFAULT FALSE,
    solar_surplus_kw DOUBLE PRECISION NOT NULL DEFAULT 0,
    telemetry_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_station_swap_state_telemetry ON station_swap_state (telemetry_updated_at);
