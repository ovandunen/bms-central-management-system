package com.solarcsms.location.application;

import com.solarcsms.location.domain.GeoCoordinate;

/**
 * CDI event representing a driver request for nearby charging stations.
 *
 * @param driverId       external driver identifier (MQTT topic segment)
 * @param coordinate     search center
 * @param radiusMeters   search radius in metres
 */
public record NearbyStationQuery(String driverId, GeoCoordinate coordinate, double radiusMeters) {
}
