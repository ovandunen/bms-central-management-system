ALTER TABLE station_location
    ADD COLUMN IF NOT EXISTS street_address VARCHAR(512);

ALTER TABLE station_location
    ADD COLUMN IF NOT EXISTS city VARCHAR(128);
