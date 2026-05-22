package com.solarcsms.location.domain;

/**
 * Immutable geographic coordinate (WGS-84).
 *
 * @param latitude  degrees north (-90..90)
 * @param longitude degrees east (-180..180)
 */
public record GeoCoordinate(double latitude, double longitude) {

    /**
     * Validates coordinate ranges.
     *
     * @return this coordinate
     * @throws IllegalArgumentException if lat/lon are out of range
     */
    public GeoCoordinate {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude out of range: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude out of range: " + longitude);
        }
    }
}
