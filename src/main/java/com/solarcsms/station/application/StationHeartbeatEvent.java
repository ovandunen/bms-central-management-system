package com.solarcsms.station.application;

import java.time.Instant;

/**
 * Domain event fired when an OCPP Heartbeat is received.
 *
 * @param stationId   OCPP charge point identifier
 * @param heartbeatAt server timestamp applied to the heartbeat
 */
public record StationHeartbeatEvent(String stationId, Instant heartbeatAt) {
}
