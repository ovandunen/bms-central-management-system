package ch.ecocarsolaire.csms.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Swap-specific telemetry for a charging station (inventory, bay status, solar surplus).
 */
@Entity
@Table(name = "station_swap_state")
public class StationSwapState {

    @Id
    @Column(name = "station_id", nullable = false, length = 64)
    private String stationId;

    @Column(name = "charged_packs_ready", nullable = false)
    private int chargedPacksReady;

    @Column(name = "swap_bay_free", nullable = false)
    private boolean swapBayFree;

    @Column(name = "solar_surplus_kw", nullable = false)
    private double solarSurplusKw;

    @Column(name = "telemetry_updated_at", nullable = false)
    private Instant telemetryUpdatedAt;

    protected StationSwapState() {
        // JPA
    }

    public StationSwapState(
            String stationId,
            int chargedPacksReady,
            boolean swapBayFree,
            double solarSurplusKw,
            Instant telemetryUpdatedAt) {
        this.stationId = stationId;
        this.chargedPacksReady = chargedPacksReady;
        this.swapBayFree = swapBayFree;
        this.solarSurplusKw = solarSurplusKw;
        this.telemetryUpdatedAt = telemetryUpdatedAt;
    }

    public String getStationId() {
        return stationId;
    }

    public int getChargedPacksReady() {
        return chargedPacksReady;
    }

    public boolean isSwapBayFree() {
        return swapBayFree;
    }

    public double getSolarSurplusKw() {
        return solarSurplusKw;
    }

    public Instant getTelemetryUpdatedAt() {
        return telemetryUpdatedAt;
    }
}
