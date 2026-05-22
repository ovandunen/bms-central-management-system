package com.solarcsms.station.domain;

/**
 * Operational status of a charging station aggregate.
 */
public enum StationStatus {
    AVAILABLE,
    OCCUPIED,
    FAULTED,
    OFFLINE,
    UNKNOWN
}
