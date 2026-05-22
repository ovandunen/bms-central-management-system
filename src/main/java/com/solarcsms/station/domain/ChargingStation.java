package com.solarcsms.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for a physical OCPP charging station.
 */
@Entity
@Table(name = "charging_station")
public class ChargingStation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "station_id", nullable = false, unique = true, length = 64)
    private String stationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StationStatus status;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "connector_count", nullable = false)
    private int connectorCount;

    protected ChargingStation() {
        // JPA
    }

    /**
     * Creates a new charging station aggregate.
     *
     * @param stationId       OCPP charge point identifier
     * @param connectorCount  number of connectors on the station
     */
    public ChargingStation(String stationId, int connectorCount) {
        this.id = UUID.randomUUID();
        this.stationId = stationId;
        this.status = StationStatus.UNKNOWN;
        this.connectorCount = connectorCount;
    }

    public UUID getId() {
        return id;
    }

    public String getStationId() {
        return stationId;
    }

    public StationStatus getStatus() {
        return status;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public int getConnectorCount() {
        return connectorCount;
    }

    /**
     * Updates heartbeat timestamp and marks station as available when appropriate.
     *
     * @param heartbeatAt server time of the heartbeat
     */
    public void recordHeartbeat(Instant heartbeatAt) {
        this.lastHeartbeat = heartbeatAt;
        if (status == StationStatus.OFFLINE || status == StationStatus.UNKNOWN) {
            this.status = StationStatus.AVAILABLE;
        }
    }

    /**
     * Applies a new operational status from domain events.
     *
     * @param newStatus status to apply
     */
    public void updateStatus(StationStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * Marks the station as offline (e.g. WebSocket disconnected).
     */
    public void markOffline() {
        this.status = StationStatus.OFFLINE;
    }
}
