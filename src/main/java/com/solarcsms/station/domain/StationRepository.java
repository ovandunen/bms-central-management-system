package com.solarcsms.station.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for {@link ChargingStation} aggregates.
 */
@ApplicationScoped
public class StationRepository implements PanacheRepositoryBase<ChargingStation, UUID> {

    /**
     * Finds a station by its OCPP charge point identifier.
     *
     * @param stationId OCPP chargePointId
     * @return matching station if present
     */
    public Optional<ChargingStation> findByStationId(String stationId) {
        return find("stationId", stationId).firstResultOptional();
    }
}
