CREATE TABLE swap_recommendation (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(64) NOT NULL UNIQUE,
    vehicle_id VARCHAR(64) NOT NULL,
    station_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    confidence DOUBLE PRECISION,
    reason TEXT
);

CREATE TABLE swap_feedback (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(64) NOT NULL REFERENCES swap_recommendation(correlation_id),
    vehicle_id VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    station_id VARCHAR(64),
    actual_swap_time TIMESTAMPTZ
);

CREATE INDEX idx_swap_feedback_processed ON swap_feedback (processed);
