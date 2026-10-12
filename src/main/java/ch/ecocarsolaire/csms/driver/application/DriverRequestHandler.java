package ch.ecocarsolaire.csms.driver.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ch.ecocarsolaire.csms.driver.domain.LocationRequest;
import ch.ecocarsolaire.csms.location.application.NearbyStationQuery;
import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consumes driver MQTT location requests and fires {@link NearbyStationQuery} CDI events.
 */
@ApplicationScoped
public class DriverRequestHandler {

    private static final Logger LOG = Logger.getLogger(DriverRequestHandler.class);
    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^drivers/([^/]+)/request/location$");

    private final Event<NearbyStationQuery> nearbyStationQueryEvent;
    private final ObjectMapper objectMapper;

    public DriverRequestHandler(Event<NearbyStationQuery> nearbyStationQueryEvent,
                                 ObjectMapper objectMapper) {
        this.nearbyStationQueryEvent = nearbyStationQueryEvent;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles inbound MQTT messages on {@code drivers/+/request/location}.
     *
     * @param msg reactive messaging envelope with raw JSON payload
     * @return completion stage acknowledging the message
     */
    @Incoming("driver-location-request")
    @Blocking
    public CompletionStage<Void> handle(Message<byte[]> msg) {
        try {
            Optional<String> driverIdFromTopic = msg.getMetadata(MqttMessage.class)
                    .map(MqttMessage::getTopic)
                    .flatMap(topic -> {
                        Matcher matcher = TOPIC_PATTERN.matcher(topic);
                        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
                    });

            LocationRequest request = objectMapper.readValue(msg.getPayload(), LocationRequest.class);
            String driverId = driverIdFromTopic.orElse(request.driverId());
            LocationRequest resolved = new LocationRequest(
                    driverId,
                    request.latitude(),
                    request.longitude(),
                    request.radiusMeters());

            validate(resolved);
            String correlationId = UUID.randomUUID().toString();
            nearbyStationQueryEvent.fire(new NearbyStationQuery(
                    resolved.driverId(),
                    new GeoCoordinate(resolved.latitude(), resolved.longitude()),
                    resolved.radiusMeters(),
                    correlationId));
            LOG.debugf("Fired NearbyStationQuery for driver %s (correlationId=%s)", driverId, correlationId);
        } catch (Exception e) {
            LOG.errorf(e, "Invalid driver location request");
        }
        return msg.ack();
    }

    private void validate(LocationRequest request) {
        new GeoCoordinate(request.latitude(), request.longitude());
        if (request.radiusMeters() < 0) {
            throw new IllegalArgumentException("radiusMeters must be non-negative");
        }
        // TODO: authenticate driverId and enforce rate limits
    }
}
