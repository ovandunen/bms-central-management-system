package ch.ecocarsolaire.csms.swap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted swap recommendation for feedback correlation and audit.
 */
@Entity
@Table(name = "swap_recommendation")
public class SwapRecommendationRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 64)
    private String correlationId;

    @Column(name = "vehicle_id", nullable = false, length = 64)
    private String vehicleId;

    @Column(name = "station_id", nullable = false, length = 64)
    private String stationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "confidence")
    private double confidence;

    @Column(name = "reason")
    private String reason;

    protected SwapRecommendationRecord() {
    }

    public SwapRecommendationRecord(
            String correlationId,
            String vehicleId,
            String stationId,
            Instant createdAt,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil,
            double confidence,
            String reason) {
        this.id = UUID.randomUUID();
        this.correlationId = correlationId;
        this.vehicleId = vehicleId;
        this.stationId = stationId;
        this.createdAt = createdAt;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.confidence = confidence;
        this.reason = reason;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getStationId() {
        return stationId;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }
}
