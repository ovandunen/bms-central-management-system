package ch.ecocarsolaire.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.Map;

/**
 * Switches MQTT channels to the in-memory connector for unit tests.
 */
public class InMemoryMessagingTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        InMemoryConnector.switchIncomingChannelsToInMemory(
                "driver-location-request", "vehicle-telemetry", "swap-feedback");
        InMemoryConnector.switchOutgoingChannelsToInMemory("station-location-response");
        return Map.of();
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
