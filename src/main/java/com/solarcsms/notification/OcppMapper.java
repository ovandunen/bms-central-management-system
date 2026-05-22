package com.solarcsms.notification;

import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import org.mapstruct.Mapper;

import java.time.Instant;

/**
 * MapStruct mapper for OCPP wire types (notification ACL only).
 */
@Mapper(componentModel = "cdi")
public interface OcppMapper {

    /**
     * Extracts connector id from a status notification.
     *
     * @param request OCPP status notification
     * @return connector id
     */
    default int connectorId(StatusNotificationRequest request) {
        return request.getConnectorId();
    }

    /**
     * Maps OCPP timestamp to domain instant (falls back to now).
     *
     * @param request status notification
     * @return occurrence time
     */
    default Instant occurredAt(StatusNotificationRequest request) {
        return Instant.now();
        // TODO: parse request.getTimestamp() when present on the wire payload
    }
}
