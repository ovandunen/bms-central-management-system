package com.solarcsms.location.infrastructure;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.reactive.messaging.mqtt.session.MqttClientSession;
import io.smallrye.reactive.messaging.mqtt.session.MqttClientSessionOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Publishes MQTT messages to dynamic topics (SmallRye 4.x replacement for legacy {@code MqttClient}).
 */
@ApplicationScoped
public class DynamicMqttPublisher {

    private static final Logger LOG = Logger.getLogger(DynamicMqttPublisher.class);

    private final MqttClientSession mqttSession;

    public DynamicMqttPublisher(Vertx vertx,
                                 @ConfigProperty(name = "mp.messaging.connector.smallrye-mqtt.host", defaultValue = "localhost")
                                 String host,
                                 @ConfigProperty(name = "mp.messaging.connector.smallrye-mqtt.port", defaultValue = "1883")
                                 int port,
                                 @ConfigProperty(name = "mp.messaging.connector.smallrye-mqtt.client-id", defaultValue = "solar-csms")
                                 String clientId) {
        MqttClientSessionOptions options = new MqttClientSessionOptions()
                .setHostname(host)
                .setPort(port)
                .setClientId(clientId + "-publisher")
                .setCleanSession(true);
        this.mqttSession = MqttClientSession.create(vertx, options);
        this.mqttSession.start()
                .onFailure(err -> LOG.warnf(err, "MQTT publisher session failed to start — dynamic publishes may fail until broker is up"));
    }

    /**
     * Publishes a payload to the given topic with QoS 1.
     *
     * @param topic   MQTT topic
     * @param payload message body
     */
    public void publish(String topic, byte[] payload) {
        mqttSession.publish(topic, Buffer.buffer(payload), MqttQoS.AT_LEAST_ONCE)
                .onFailure(err -> LOG.errorf(err, "MQTT publish failed for topic %s", topic));
    }

    @PreDestroy
    void stop() {
        mqttSession.stop();
    }
}
