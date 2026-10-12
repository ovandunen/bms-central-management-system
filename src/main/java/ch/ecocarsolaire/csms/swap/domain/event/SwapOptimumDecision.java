package ch.ecocarsolaire.csms.swap.domain.event;

import java.time.OffsetDateTime;

/**
 * Domain event emitted when CSMS decides the optimum moment for a physical battery swap.
 *
 * @param vehicleId   vehicle identifier
 * @param stationId   recommended swap station
 * @param validFrom   start of swap window
 * @param validUntil  end of swap window
 * @param confidence  blended decision confidence (0.0–1.0)
 * @param finalScore  blended KI + rule score (0.0–1.0)
 * @param reason      audit reason for the decision
 * @param routeEta    estimated minutes until vehicle reaches station
 */
public record SwapOptimumDecision(
        String correlationId,
        String vehicleId,
        String stationId,
        OffsetDateTime validFrom,
        OffsetDateTime validUntil,
        double confidence,
        double finalScore,
        String reason,
        int routeEta) {
}
