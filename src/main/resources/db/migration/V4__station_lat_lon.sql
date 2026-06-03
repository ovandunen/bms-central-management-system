-- Add explicit lat/lon columns and keep PostGIS geometry in sync.
-- Handles DBs created from V1 (geometry only) and DBs already on lat/lon (e.g. Hibernate dev).

ALTER TABLE station_location
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;

ALTER TABLE station_location
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'station_location'
          AND column_name = 'location'
    ) THEN
        UPDATE station_location
        SET
            latitude = COALESCE(latitude, ST_Y(location::geometry)),
            longitude = COALESCE(longitude, ST_X(location::geometry))
        WHERE location IS NOT NULL
          AND (latitude IS NULL OR longitude IS NULL);
    END IF;
END $$;

UPDATE station_location
SET latitude = 52.52, longitude = 13.405
WHERE latitude IS NULL OR longitude IS NULL;

ALTER TABLE station_location
    ALTER COLUMN latitude SET NOT NULL;

ALTER TABLE station_location
    ALTER COLUMN longitude SET NOT NULL;

ALTER TABLE station_location
    ADD COLUMN IF NOT EXISTS location GEOMETRY(Point, 4326);

UPDATE station_location
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
WHERE location IS NULL;

CREATE INDEX IF NOT EXISTS idx_station_location_geo ON station_location USING GIST (location);

CREATE OR REPLACE FUNCTION station_location_sync_geom()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_station_location_sync_geom ON station_location;
CREATE TRIGGER trg_station_location_sync_geom
    BEFORE INSERT OR UPDATE OF latitude, longitude ON station_location
    FOR EACH ROW
    EXECUTE FUNCTION station_location_sync_geom();
