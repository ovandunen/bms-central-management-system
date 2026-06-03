package com.solarcsms.location.infrastructure;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.reactive.messaging.mqtt.SendingMqttMessageMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Upstream producer for {@code station-location-response}: publishes station lists to
 * {@code stations/available/{vehicleId}} on the shared SmallRye MQTT connection.
 */
@ApplicationScoped
public class MqttStationResponsePublisher {

    private static final Logger LOG = Logger.getLogger(MqttStationResponsePublisher.class);

    private final Emitter<byte[]> stationResponses;

    public MqttStationResponsePublisher(@Channel("station-location-response") Emitter<byte[]> stationResponses) {
        this.stationResponses = stationResponses;
    }

    /**
     * Publishes a payload to the given topic with QoS 1.
     *
     * @param topic   MQTT topic (e.g. {@code stations/available/vehicle-001})
     * @param payload JSON body
     */
    public void publish(String topic, byte[] payload) {
        var metadata = new SendingMqttMessageMetadata(topic, MqttQoS.AT_LEAST_ONCE, false);
        try {
            stationResponses.send(Message.of(payload).addMetadata(metadata));
        } catch (Exception e) {
            LOG.errorf(e, "MQTT publish failed for topic %s", topic);
        }
    }
}
