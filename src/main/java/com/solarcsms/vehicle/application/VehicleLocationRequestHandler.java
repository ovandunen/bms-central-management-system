package com.solarcsms.vehicle.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarcsms.location.application.NearbyStationQuery;
import com.solarcsms.location.domain.GeoCoordinate;
import com.solarcsms.vehicle.domain.VehicleLocationRequest;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consumes vehicle MQTT location requests and fires {@link NearbyStationQuery} for the location context.
 */
@ApplicationScoped
public class VehicleLocationRequestHandler {

    private static final Logger LOG = Logger.getLogger(VehicleLocationRequestHandler.class);
    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^vehicles/([^/]+)/request/location$");

    private final Event<NearbyStationQuery> nearbyStationQueryEvent;
    private final ObjectMapper objectMapper;

    public VehicleLocationRequestHandler(Event<NearbyStationQuery> nearbyStationQueryEvent,
                                          ObjectMapper objectMapper) {
        this.nearbyStationQueryEvent = nearbyStationQueryEvent;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles inbound MQTT messages on {@code vehicles/+/request/location}.
     */
    @Incoming("vehicle-location-request")
    @Blocking
    public CompletionStage<Void> handle(Message<byte[]> msg) {
        try {
            Optional<String> vehicleIdFromTopic = msg.getMetadata(MqttMessage.class)
                    .map(MqttMessage::getTopic)
                    .flatMap(topic -> {
                        Matcher matcher = TOPIC_PATTERN.matcher(topic);
                        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
                    });

            VehicleLocationRequest request =
                    objectMapper.readValue(msg.getPayload(), VehicleLocationRequest.class);
            String vehicleId = vehicleIdFromTopic.orElse(request.vehicleId());
            VehicleLocationRequest resolved = new VehicleLocationRequest(
                    vehicleId,
                    request.latitude(),
                    request.longitude(),
                    request.radiusMeters());

            validate(resolved);
            nearbyStationQueryEvent.fire(new NearbyStationQuery(
                    resolved.vehicleId(),
                    new GeoCoordinate(resolved.latitude(), resolved.longitude()),
                    resolved.radiusMeters()));
            LOG.debugf("Fired NearbyStationQuery for vehicle %s", vehicleId);
        } catch (Exception e) {
            LOG.errorf(e, "Invalid vehicle location request");
        }
        return msg.ack();
    }

    private void validate(VehicleLocationRequest request) {
        new GeoCoordinate(request.latitude(), request.longitude());
        if (request.radiusMeters() < 0) {
            throw new IllegalArgumentException("radiusMeters must be non-negative");
        }
        // TODO: authenticate vehicleId and enforce rate limits
    }
}
