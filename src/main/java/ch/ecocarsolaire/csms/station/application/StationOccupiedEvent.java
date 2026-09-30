package ch.ecocarsolaire.csms.station.application;

import java.time.Instant;

/**
 * Domain event fired when a station is occupied (preparing, charging, or occupied).
 *
 * @param stationId   OCPP charge point identifier
 * @param connectorId connector that reported occupancy
 * @param occurredAt  time of the status change
 */
public record StationOccupiedEvent(String stationId, int connectorId, Instant occurredAt) {
}
