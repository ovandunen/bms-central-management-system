package com.solarcsms.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active OCPP WebSocket sessions keyed by charge point identifier.
 */
@ApplicationScoped
public class OcppSessionRegistry {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Registers a session for a charge point.
     *
     * @param chargePointId OCPP charge point id
     * @param session       open WebSocket session
     */
    public void register(String chargePointId, Session session) {
        sessions.put(chargePointId, session);
    }

    /**
     * Removes a session for a charge point.
     *
     * @param chargePointId OCPP charge point id
     * @return previous session if any
     */
    public Optional<Session> remove(String chargePointId) {
        return Optional.ofNullable(sessions.remove(chargePointId));
    }

    /**
     * Returns the active session for a charge point.
     *
     * @param chargePointId OCPP charge point id
     * @return session if connected
     */
    public Optional<Session> get(String chargePointId) {
        return Optional.ofNullable(sessions.get(chargePointId));
    }

    /**
     * @return number of connected charge points
     */
    public int size() {
        return sessions.size();
    }
}
