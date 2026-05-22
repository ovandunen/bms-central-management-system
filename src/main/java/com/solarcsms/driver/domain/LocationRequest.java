package com.solarcsms.driver.domain;

/**
 * Value object for an inbound driver location search request.
 *
 * @param driverId       driver identifier from MQTT topic
 * @param latitude       search center latitude
 * @param longitude      search center longitude
 * @param radiusMeters   search radius in metres (0 = use server default)
 */
public record LocationRequest(String driverId, double latitude, double longitude, double radiusMeters) {
}
