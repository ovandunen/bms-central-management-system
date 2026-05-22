package com.solarcsms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarcsms.driver.domain.LocationRequest;
import com.solarcsms.support.InMemoryMessagingTestResource;
import com.solarcsms.support.NearbyStationQueryCaptor;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies MQTT driver location requests fire {@link com.solarcsms.location.application.NearbyStationQuery}.
 */
@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
class DriverRequestHandlerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    NearbyStationQueryCaptor queryCaptor;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        queryCaptor.reset();
    }

    @Test
    void mqttLocationRequestFiresNearbyStationQuery() throws Exception {
        InMemorySource<Message<byte[]>> source = connector.source("driver-location-request");
        LocationRequest request = new LocationRequest("test", 52.52, 13.405, 10_000);
        byte[] payload = objectMapper.writeValueAsBytes(request);

        source.send(Message.of(payload)
                .addMetadata(MqttMessage.of("drivers/test/request/location", payload)));

        assertTrue(awaitQuery(), "NearbyStationQuery should be observed");
        assertEquals("test", queryCaptor.lastQuery().get().driverId());
        assertEquals(52.52, queryCaptor.lastQuery().get().coordinate().latitude(), 0.001);
    }

    private boolean awaitQuery() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            if (queryCaptor.lastQuery().isPresent()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        return false;
    }
}
