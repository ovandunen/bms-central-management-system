package com.solarcsms.location.application;

import com.solarcsms.location.domain.GeoCoordinate;

/**
 * CDI event: a vehicle requested nearby charging stations (vehicle MQTT channel).
 *
 * @param vehicleId      vehicle identifier (MQTT topic segment)
 * @param coordinate     search center
 * @param radiusMeters   search radius in metres
 */
public record NearbyStationQuery(String vehicleId, GeoCoordinate coordinate, double radiusMeters) {
}
