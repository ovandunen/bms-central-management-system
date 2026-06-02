package com.solarcsms.location.application;

import com.solarcsms.location.domain.StationLocation;
import com.solarcsms.station.domain.StationStatus;

/**
 * MQTT payload record for a single nearby station (chargeable = {@link StationStatus#AVAILABLE}).
 *
 * @param stationId        OCPP charge point id
 * @param displayName      human-readable name (may be null)
 * @param streetAddress    optional street line for map/list
 * @param city             optional city
 * @param latitude         WGS-84 latitude
 * @param longitude        WGS-84 longitude
 * @param solarCapacityKw  installed solar capacity in kW
 * @param status           operational status name (e.g. AVAILABLE)
 */
public record StationLocationDto(
        String stationId,
        String displayName,
        String streetAddress,
        String city,
        double latitude,
        double longitude,
        double solarCapacityKw,
        String status) {

    /**
     * Maps a domain aggregate to a DTO suitable for JSON serialization.
     *
     * @param location station location aggregate
     * @param status   operational status from {@link com.solarcsms.station.domain.ChargingStation}
     * @return DTO instance
     */
    public static StationLocationDto from(StationLocation location, StationStatus status) {
        return new StationLocationDto(
                location.getStationId(),
                location.getDisplayName(),
                location.getStreetAddress(),
                location.getCity(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSolarCapacityKw(),
                status.name());
    }
}
