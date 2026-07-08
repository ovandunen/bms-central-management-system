package ch.ecocarsolaire.csms.swap.application.port.dto;

import java.time.OffsetDateTime;

/**
 * Response from external KI pattern-recognition optimization.
 *
 * @param score        KI optimum-window score (0.0–1.0)
 * @param validFrom    start of recommended swap window
 * @param validUntil   end of recommended swap window
 * @param confidence   model confidence (0.0–1.0)
 * @param explanation  human-readable rationale from KI
 */
public record KiOptimizationResponse(
        double score,
        OffsetDateTime validFrom,
        OffsetDateTime validUntil,
        double confidence,
        String explanation) {
}
