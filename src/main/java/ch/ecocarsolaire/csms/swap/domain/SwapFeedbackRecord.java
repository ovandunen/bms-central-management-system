package ch.ecocarsolaire.csms.swap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Driver feedback for a swap recommendation.
 */
@Entity
@Table(name = "swap_feedback")
public class SwapFeedbackRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "vehicle_id", nullable = false, length = 64)
    private String vehicleId;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "station_id", length = 64)
    private String stationId;

    @Column(name = "actual_swap_time")
    private Instant actualSwapTime;

    protected SwapFeedbackRecord() {
    }

    public SwapFeedbackRecord(
            String correlationId,
            String vehicleId,
            String state,
            Instant receivedAt,
            String stationId,
            Instant actualSwapTime) {
        this.id = UUID.randomUUID();
        this.correlationId = correlationId;
        this.vehicleId = vehicleId;
        this.state = state;
        this.receivedAt = receivedAt;
        this.processed = false;
        this.stationId = stationId;
        this.actualSwapTime = actualSwapTime;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getState() {
        return state;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void markProcessed() {
        this.processed = true;
    }
}
