package com.solarcsms.location.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for {@link StationLocation} with PostGIS spatial queries.
 */
@ApplicationScoped
public class LocationRepository implements PanacheRepositoryBase<StationLocation, UUID> {

    @PersistenceContext
    EntityManager entityManager;

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
    @SuppressWarnings("unchecked")
    public List<StationLocation> findNearby(double latitude, double longitude, double radiusMeters) {
        return entityManager.createNativeQuery("""
                SELECT sl.*
                FROM station_location sl
                WHERE ST_DWithin(
                    sl.location::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :radius
                )
                ORDER BY ST_Distance(
                    sl.location::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                )
                """, StationLocation.class)
                .setParameter("lat", latitude)
                .setParameter("lon", longitude)
                .setParameter("radius", radiusMeters)
                .getResultList();
    }
}
