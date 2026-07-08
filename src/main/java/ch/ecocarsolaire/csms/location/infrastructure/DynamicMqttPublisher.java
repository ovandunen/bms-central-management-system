package ch.ecocarsolaire.csms.location.infrastructure;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.mqtt.session.MqttClientSession;
import io.smallrye.reactive.messaging.mqtt.session.MqttClientSessionOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Publishes MQTT messages to dynamic topics (SmallRye 4.x replacement for legacy {@code MqttClient}).
 */
@ApplicationScoped
public class DynamicMqttPublisher {

    private static final Logger LOG = Logger.getLogger(DynamicMqttPublisher.class);
    private static final int PUBLISH_RETRY_DELAY_MS = 250;
    private static final int MAX_PUBLISH_ATTEMPTS = 8;

    private final Vertx vertx;
    private final MqttClientSession mqttSession;

    public DynamicMqttPublisher(
            Vertx vertx,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-mqtt.host", defaultValue = "localhost")
            String host,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-mqtt.port", defaultValue = "1883")
            int port,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-mqtt.client-id", defaultValue = "solar-csms")
            String clientId) {
        this.vertx = vertx;
        MqttClientSessionOptions options = new MqttClientSessionOptions()
                .setHostname(host)
                .setPort(port)
                .setClientId(clientId + "-publisher")
                .setCleanSession(true);
        this.mqttSession = MqttClientSession.create(vertx, options);
    }

    void onStart(@Observes StartupEvent event) {
        try {
            mqttSession.start().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
            LOG.info("MQTT publisher session connected");
        } catch (Exception e) {
            LOG.warnf(e, "MQTT publisher session failed to start — dynamic publishes will retry");
        }
    }

    /**
     * Publishes a payload to the given topic with QoS 1.
     *
     * @param topic   MQTT topic
     * @param payload message body
     */
    public void publish(String topic, byte[] payload) {
        publishWithRetry(topic, payload, MAX_PUBLISH_ATTEMPTS);
    }

    private void publishWithRetry(String topic, byte[] payload, int attemptsLeft) {
        mqttSession.publish(topic, Buffer.buffer(payload), MqttQoS.AT_LEAST_ONCE)
                .onSuccess(ignored -> LOG.debugf("MQTT published to %s", topic))
                .onFailure(err -> {
                    if (attemptsLeft > 1) {
                        LOG.debugf("MQTT publish retry for %s (%s attempts left): %s",
                                topic, attemptsLeft - 1, err.getMessage());
                        vertx.setTimer(PUBLISH_RETRY_DELAY_MS,
                                id -> publishWithRetry(topic, payload, attemptsLeft - 1));
                    } else {
                        LOG.errorf(err, "MQTT publish failed for topic %s", topic);
                    }
                });
    }

    @PreDestroy
    void stop() {
        mqttSession.stop();
    }
}
