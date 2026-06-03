package com.solarcsms.location.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for {@link StationLocation} with PostGIS spatial queries.
 */
@ApplicationScoped
public class LocationRepository implements PanacheRepositoryBase<StationLocation, UUID> {

    private static final Logger LOG = Logger.getLogger(LocationRepository.class);
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    @PersistenceContext
    EntityManager entityManager;

    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "postgresql")
    String dbKind;

    /**
     * Finds a location by OCPP station identifier.
     *
     * @param stationId charge point id
     * @return matching location if present
     */
    public Optional<StationLocation> findByStationId(String stationId) {
        return find("stationId", stationId).firstResultOptional();
    }

    /**
     * Returns stations within {@code radiusMeters} of the given WGS-84 coordinate.
     *
     * @param latitude       search center latitude
     * @param longitude      search center longitude
     * @param radiusMeters   search radius in metres
     * @return stations inside the radius, ordered by distance
     */
    public List<StationLocation> findNearby(double latitude, double longitude, double radiusMeters) {
        if (usePostgisNative()) {
            return findNearbyPostgis(latitude, longitude, radiusMeters);
        }
        LOG.debugf("Using in-memory Haversine search (db-kind=%s); use PostgreSQL+PostGIS in production", dbKind);
        return findNearbyHaversine(latitude, longitude, radiusMeters);
    }

    private boolean usePostgisNative() {
        return "postgresql".equalsIgnoreCase(dbKind);
    }

    @SuppressWarnings("unchecked")
    private List<StationLocation> findNearbyPostgis(double latitude, double longitude, double radiusMeters) {
        return entityManager.createNativeQuery("""
                SELECT sl.*
                FROM station_location sl
                WHERE ST_DWithin(
                    ST_SetSRID(ST_MakePoint(sl.longitude, sl.latitude), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :radius
                )
                ORDER BY ST_Distance(
                    ST_SetSRID(ST_MakePoint(sl.longitude, sl.latitude), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                )
                """, StationLocation.class)
                .setParameter("lat", latitude)
                .setParameter("lon", longitude)
                .setParameter("radius", radiusMeters)
                .getResultList();
    }

    private List<StationLocation> findNearbyHaversine(double latitude, double longitude, double radiusMeters) {
        return findAll().stream()
                .filter(sl -> haversineMeters(latitude, longitude, sl.getLatitude(), sl.getLongitude()) <= radiusMeters)
                .sorted(Comparator.comparingDouble(sl ->
                        haversineMeters(latitude, longitude, sl.getLatitude(), sl.getLongitude())))
                .toList();
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
