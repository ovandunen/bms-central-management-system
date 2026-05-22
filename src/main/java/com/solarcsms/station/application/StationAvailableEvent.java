package com.solarcsms.station.application;

import java.time.Instant;

/**
 * Domain event fired when a station becomes available for charging.
 *
 * @param stationId     OCPP charge point identifier
 * @param connectorId   connector that reported availability
 * @param occurredAt    time of the status change
 */
public record StationAvailableEvent(String stationId, int connectorId, Instant occurredAt) {
}
