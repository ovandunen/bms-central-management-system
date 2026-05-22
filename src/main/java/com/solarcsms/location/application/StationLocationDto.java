package com.solarcsms.location.application;

import com.solarcsms.location.domain.StationLocation;

/**
 * MQTT payload record for a single nearby station.
 *
 * @param stationId        OCPP charge point id
 * @param displayName      human-readable name
 * @param latitude         WGS-84 latitude
 * @param longitude        WGS-84 longitude
 * @param solarCapacityKw  installed solar capacity in kW
 */
public record StationLocationDto(
        String stationId,
        String displayName,
        double latitude,
        double longitude,
        double solarCapacityKw) {

    /**
     * Maps a domain aggregate to a DTO suitable for JSON serialization.
     *
     * @param location station location aggregate
     * @return DTO instance
     */
    public static StationLocationDto from(StationLocation location) {
        return new StationLocationDto(
                location.getStationId(),
                location.getDisplayName(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSolarCapacityKw());
    }
}
