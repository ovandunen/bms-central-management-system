package com.solarcsms.station.application;

import java.time.Instant;

/**
 * Fired when an OCPP BootNotification is accepted for a charge point.
 *
 * @param stationId OCPP charge point identifier
 * @param vendor    charge point vendor
 * @param model     charge point model
 * @param occurredAt server timestamp
 */
public record StationRegisteredEvent(String stationId, String vendor, String model, Instant occurredAt) {
}
