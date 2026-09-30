package ch.ecocarsolaire.csms.swap.infrastructure.emit;

import java.time.OffsetDateTime;

/**
 * MQTT payload for swap recommendation published to BMS monitoring (UC-06).
 *
 * @param stationId  recommended swap station
 * @param latitude   WGS-84 latitude of recommended station (from {@code station_location})
 * @param longitude  WGS-84 longitude of recommended station (from {@code station_location})
 * @param validFrom  start of swap window
 * @param validUntil end of swap window
 * @param reason     audit reason for the decision
 * @param confidence blended decision confidence (0.0–1.0)
 * @param routeEta   estimated minutes until vehicle reaches station
 */
public record SwapRecommendationMqttPayload(
        String correlationId,
        String stationId,
        double latitude,
        double longitude,
        OffsetDateTime validFrom,
        OffsetDateTime validUntil,
        String reason,
        double confidence,
        int routeEta) {
}
