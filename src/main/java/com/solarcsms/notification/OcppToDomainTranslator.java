package com.solarcsms.notification;

import com.solarcsms.station.application.StationAvailableEvent;
import com.solarcsms.station.application.StationOfflineEvent;
import com.solarcsms.station.application.StationRegisteredEvent;

import eu.chargetime.ocpp.model.core.ChargePointStatus;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;

/**
 * Anti-corruption layer: translates OCPP {@link StatusNotificationRequest} into domain CDI events.
 */
@ApplicationScoped
public class OcppToDomainTranslator {

    private static final Logger LOG = Logger.getLogger(OcppToDomainTranslator.class);

    private final OcppMapper ocppMapper;
    private final Event<StationAvailableEvent> availableEvent;
    private final Event<StationOfflineEvent> offlineEvent;
    private final Event<StationRegisteredEvent> registeredEvent;

    public OcppToDomainTranslator(OcppMapper ocppMapper,
                                   Event<StationAvailableEvent> availableEvent,
                                   Event<StationOfflineEvent> offlineEvent,
                                   Event<StationRegisteredEvent> registeredEvent) {
        this.ocppMapper = ocppMapper;
        this.availableEvent = availableEvent;
        this.offlineEvent = offlineEvent;
        this.registeredEvent = registeredEvent;
    }

    /**
     * Translates a status notification into domain events and fires them asynchronously.
     *
     * @param chargePointId OCPP charge point identifier
     * @param request       parsed status notification payload
     */
    public void translateStatusNotification(String chargePointId, StatusNotificationRequest request) {
        Instant occurredAt = ocppMapper.occurredAt(request);
        int connectorId = ocppMapper.connectorId(request);
        ChargePointStatus status = request.getStatus();

        LOG.debugf("StatusNotification from %s connector %d: %s", chargePointId, connectorId, status);

        if (status == ChargePointStatus.Available) {
            availableEvent.fire(new StationAvailableEvent(chargePointId, connectorId, occurredAt));
            return;
        }

        if (status == ChargePointStatus.Unavailable || status == ChargePointStatus.Faulted) {
            offlineEvent.fire(new StationOfflineEvent(
                    chargePointId,
                    "OCPP status: " + status,
                    occurredAt));
        }
        // TODO: map Occupied, Preparing, Charging, etc. to StationStatus.OCCUPIED
    }

    /**
     * Fires an offline event when the WebSocket session ends.
     *
     * @param chargePointId OCPP charge point identifier
     * @param reason        disconnect reason
     */
    public void translateSessionClosed(String chargePointId, String reason) {
        offlineEvent.fire(new StationOfflineEvent(chargePointId, reason, Instant.now()));
    }

    /**
     * Optionally maps boot notification metadata for future provisioning.
     *
     * @param chargePointId charge point id
     * @param vendor        charge point vendor
     * @param model         charge point model
     * @return empty — skeleton placeholder
     */
    public Optional<Void> translateBootNotification(String chargePointId, String vendor, String model) {
        LOG.infof("BootNotification from %s (%s / %s)", chargePointId, vendor, model);
        registeredEvent.fire(new StationRegisteredEvent(
                chargePointId,
                vendor,
                model,
                Instant.now()));
        return Optional.empty();
    }
}
