package ch.ecocarsolaire.csms.swap.application.port.dto;

import java.time.Instant;

/**
 * MQTT payload for driver swap feedback.
 */
public record SwapFeedbackMessage(
        String vehicleId,
        String correlationId,
        String state,
        String stationId,
        Instant actualSwapTime) {
}
