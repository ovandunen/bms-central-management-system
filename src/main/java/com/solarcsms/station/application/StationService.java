package com.solarcsms.station.application;

import com.solarcsms.station.domain.ChargingStation;
import com.solarcsms.station.domain.StationRepository;
import com.solarcsms.station.domain.StationStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Application service for the station bounded context.
 * Reacts to cross-context CDI events — never called directly from other contexts.
 */
@ApplicationScoped
public class StationService {

    private static final Logger LOG = Logger.getLogger(StationService.class);

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    /**
     * Handles station availability transitions from the notification anti-corruption layer.
     *
     * @param event availability domain event
     */
    @Transactional
    public void onStationAvailable(@Observes StationAvailableEvent event) {
        LOG.infof("Station available: %s connector %d", event.stationId(), event.connectorId());
        ChargingStation station = stationRepository.findByStationId(event.stationId())
                .orElseGet(() -> new ChargingStation(event.stationId(), 1));
        station.updateStatus(StationStatus.AVAILABLE);
        station.recordHeartbeat(event.occurredAt());
        stationRepository.persist(station);
        // TODO: emit integration events for billing / roaming when required
    }

    /**
     * Handles station offline transitions (WebSocket close or fault).
     *
     * @param event offline domain event
     */
    @Transactional
    public void onStationOffline(@Observes StationOfflineEvent event) {
        LOG.infof("Station offline: %s — %s", event.stationId(), event.reason());
        stationRepository.findByStationId(event.stationId()).ifPresent(station -> {
            station.markOffline();
            stationRepository.persist(station);
        });
    }

    /**
     * Records an OCPP heartbeat delivered via CDI from the notification context.
     *
     * @param event heartbeat domain event
     */
    @Transactional
    public void onHeartbeat(@Observes StationHeartbeatEvent event) {
        ChargingStation station = stationRepository.findByStationId(event.stationId())
                .orElseGet(() -> new ChargingStation(event.stationId(), 1));
        station.recordHeartbeat(event.heartbeatAt());
        stationRepository.persist(station);
    }
}
