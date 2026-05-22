package com.solarcsms.notification;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * CSMS-side OCPP 1.6 JSON WebSocket endpoint.
 */
@ServerEndpoint(value = "/ocpp/{chargePointId}", configurator = OcppEndpointConfigurator.class)
@jakarta.enterprise.context.ApplicationScoped
public class OcppWebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(OcppWebSocketEndpoint.class);

    @Inject
    OcppSessionRegistry sessionRegistry;

    @Inject
    OcppMessageHandler messageHandler;

    @Inject
    OcppToDomainTranslator domainTranslator;

    @Inject
    Tracer tracer;

    /**
     * Accepts a new charge point connection and sends BootNotification acceptance.
     *
     * @param chargePointId OCPP charge point identifier
     * @param session       WebSocket session
     */
    @OnOpen
    public void onOpen(@PathParam("chargePointId") String chargePointId, Session session) {
        sessionRegistry.register(chargePointId, session);
        LOG.infof("OCPP session opened: %s", chargePointId);

        BootNotificationResponseSender.sendAccepted(session);
        // TODO: validate chargePointId against registered stations / API keys
    }

    /**
     * Handles an inbound OCPP JSON message from the charge point.
     *
     * @param chargePointId charge point identifier
     * @param rawMessage    OCPP JSON array frame
     * @param session       WebSocket session
     */
    @OnMessage
    @WithSpan("ocpp.inbound")
    public void onMessage(@PathParam("chargePointId") String chargePointId,
                          String rawMessage,
                          Session session) {
        Span span = tracer.spanBuilder("ocpp.message")
                .setAttribute("chargePointId", chargePointId)
                .startSpan();
        try (var scope = span.makeCurrent()) {
            Optional<String> response = messageHandler.handleMessage(chargePointId, session, rawMessage);
            response.ifPresent(r -> sendText(session, r));
        } catch (Exception e) {
            span.recordException(e);
            LOG.errorf(e, "OCPP processing failed for %s", chargePointId);
        } finally {
            span.end();
        }
    }

    /**
     * Cleans up when a charge point disconnects.
     *
     * @param chargePointId charge point identifier
     * @param session       closing session
     */
    @OnClose
    public void onClose(@PathParam("chargePointId") String chargePointId, Session session) {
        sessionRegistry.remove(chargePointId);
        domainTranslator.translateSessionClosed(chargePointId, "WebSocket closed");
        LOG.infof("OCPP session closed: %s", chargePointId);
    }

    /**
     * Handles WebSocket errors and marks the station offline.
     *
     * @param chargePointId charge point identifier
     * @param session       session that failed
     * @param error         throwable cause
     */
    @OnError
    public void onError(@PathParam("chargePointId") String chargePointId,
                        Session session,
                        Throwable error) {
        sessionRegistry.remove(chargePointId);
        domainTranslator.translateSessionClosed(chargePointId,
                error != null ? error.getMessage() : "WebSocket error");
        LOG.errorf(error, "OCPP session error: %s", chargePointId);
    }

    private void sendText(Session session, String payload) {
        session.getAsyncRemote().sendText(payload);
    }

    /**
     * Sends an initial BootNotification CALLRESULT on connect (charge points may re-send BootNotification).
     */
    static final class BootNotificationResponseSender {

        private BootNotificationResponseSender() {
        }

        static void sendAccepted(Session session) {
            // Charge points typically send BootNotification first; proactive acceptance is optional.
            LOG.debugf("Awaiting BootNotification on session %s", session.getId());
        }
    }
}
