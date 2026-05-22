package com.solarcsms.notification;

import io.quarkus.arc.Arc;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * CDI-aware configurator so {@link OcppWebSocketEndpoint} receives injected dependencies.
 */
public class OcppEndpointConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return Arc.container().select(endpointClass).get();
    }
}
