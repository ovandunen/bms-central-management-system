package com.solarcsms.vehicle.domain;

/**
 * Inbound MQTT payload: the vehicle (BMS / VCU path) requests nearby charge points.
 *
 * @param vehicleId      vehicle identifier from MQTT topic
 * @param latitude       WGS-84 latitude
 * @param longitude      WGS-84 longitude
 * @param radiusMeters   search radius in metres
 */
public record VehicleLocationRequest(
        String vehicleId,
        double latitude,
        double longitude,
        double radiusMeters) {
}
