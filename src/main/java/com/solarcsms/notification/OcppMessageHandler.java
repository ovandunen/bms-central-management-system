package com.solarcsms.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarcsms.station.application.StationHeartbeatEvent;

import eu.chargetime.ocpp.JSONCommunicator;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.BootNotificationRequest;
import eu.chargetime.ocpp.model.core.HeartbeatConfirmation;
import eu.chargetime.ocpp.model.core.RegistrationStatus;
import eu.chargetime.ocpp.model.core.StatusNotificationConfirmation;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Handles inbound OCPP CALL frames with fault tolerance and delegates to domain translators.
 */
@ApplicationScoped
public class OcppMessageHandler {

    private static final Logger LOG = Logger.getLogger(OcppMessageHandler.class);

    private final JSONCommunicator communicator;
    private final OcppToDomainTranslator translator;
    private final Event<StationHeartbeatEvent> heartbeatEvent;
    private final ObjectMapper objectMapper;

    @Inject
    public OcppMessageHandler(OcppToDomainTranslator translator,
                                Event<StationHeartbeatEvent> heartbeatEvent,
                                ObjectMapper objectMapper) {
        this.communicator = new JSONCommunicator(new NoOpRadio());
        this.translator = translator;
        this.heartbeatEvent = heartbeatEvent;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a raw OCPP JSON CALL frame and returns the CALLRESULT JSON string.
     *
     * @param chargePointId charge point path parameter
     * @param session       WebSocket session for replies
     * @param rawMessage    raw JSON array from the charge point
     * @return CALLRESULT or CALLERROR JSON, if a response is required
     */
    @CircuitBreaker(requestVolumeThreshold = 5, delay = 30, failOn = Exception.class)
    public Optional<String> handleMessage(String chargePointId, Session session, String rawMessage) {
        try {
            JsonNode frame = objectMapper.readTree(rawMessage);
            if (!frame.isArray() || frame.size() < 3) {
                return Optional.of(packCallError("unknown", "FormationViolation",
                        "Invalid OCPP frame"));
            }

            int messageType = frame.get(0).asInt();
            if (messageType != 2) {
                LOG.debugf("Ignoring non-CALL frame type %d from %s", messageType, chargePointId);
                return Optional.empty();
            }

            String messageId = frame.get(1).asText();
            String action = frame.get(2).asText();
            JsonNode payloadNode = frame.size() > 3 ? frame.get(3) : objectMapper.createObjectNode();

            return switch (action) {
                case "BootNotification" -> handleBootNotification(chargePointId, messageId, payloadNode);
                case "Heartbeat" -> handleHeartbeat(chargePointId, messageId);
                case "StatusNotification" -> handleStatusNotification(chargePointId, messageId, payloadNode);
                default -> Optional.of(packCallError(messageId, "NotImplemented",
                        "Action not supported: " + action));
            };
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process OCPP message from %s", chargePointId);
            throw new RuntimeException(e);
        }
    }

    private Optional<String> handleBootNotification(String chargePointId, String messageId, JsonNode payloadNode)
            throws Exception {
        BootNotificationRequest request = communicator.unpackPayload(payloadNode, BootNotificationRequest.class);
        translator.translateBootNotification(
                chargePointId,
                request.getChargePointVendor(),
                request.getChargePointModel());

        BootNotificationConfirmation confirmation = new BootNotificationConfirmation();
        confirmation.setStatus(RegistrationStatus.Accepted);
        confirmation.setCurrentTime(ZonedDateTime.now(ZoneOffset.UTC));
        confirmation.setInterval(300);
        return Optional.of(packCallResult(messageId, confirmation));
    }

    private Optional<String> handleHeartbeat(String chargePointId, String messageId) {
        heartbeatEvent.fire(new StationHeartbeatEvent(chargePointId, Instant.now()));
        HeartbeatConfirmation confirmation = new HeartbeatConfirmation();
        confirmation.setCurrentTime(ZonedDateTime.now(ZoneOffset.UTC));
        return Optional.of(packCallResult(messageId, confirmation));
    }

    private Optional<String> handleStatusNotification(String chargePointId, String messageId, JsonNode payloadNode)
            throws Exception {
        StatusNotificationRequest request =
                communicator.unpackPayload(payloadNode, StatusNotificationRequest.class);
        translator.translateStatusNotification(chargePointId, request);
        return Optional.of(packCallResult(messageId, new StatusNotificationConfirmation()));
    }

    private String packCallResult(String messageId, Confirmation confirmation) {
        return (String) communicator.packPayload(new Object[]{3, messageId, confirmation});
    }

    private String packCallError(String messageId, String errorCode, String description) {
        return (String) communicator.packPayload(new Object[]{
                4, messageId, errorCode, description, objectMapper.createObjectNode()
        });
    }
}
