package com.solarcsms.station.application;

import java.time.Instant;

/**
 * Domain event fired when a station goes offline.
 *
 * @param stationId  OCPP charge point identifier
 * @param reason     human-readable disconnect reason
 * @param occurredAt time of the offline transition
 */
public record StationOfflineEvent(String stationId, String reason, Instant occurredAt) {
}
