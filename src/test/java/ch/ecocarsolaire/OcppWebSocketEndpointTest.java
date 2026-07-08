package ch.ecocarsolaire;

import ch.ecocarsolaire.csms.station.application.StationAvailableEvent;
import ch.ecocarsolaire.support.StationAvailableEventCaptor;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies OCPP StatusNotification triggers a {@link StationAvailableEvent}.
 */
@QuarkusTest
@Tag("unit")
class OcppWebSocketEndpointTest {

    @Inject
    StationAvailableEventCaptor captor;

    @BeforeEach
    void reset() {
        captor.reset();
    }

    @Test
    void statusNotificationFiresStationAvailableEvent() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        URI uri = URI.create("ws://localhost:8081/ocpp/TEST-CP-001");

        var container = jakarta.websocket.ContainerProvider.getWebSocketContainer();
        var session = container.connectToServer(new OcppTestClient(connected), uri);

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        assertTrue(awaitEvent(), "StationAvailableEvent should have been fired");
        session.close();

        assertEquals("TEST-CP-001", captor.lastEvent().get().stationId());
    }

    private boolean awaitEvent() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            if (captor.lastEvent().isPresent()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        return false;
    }

    @Vetoed
    @ClientEndpoint
    static class OcppTestClient {

        private final CountDownLatch connected;

        OcppTestClient(CountDownLatch connected) {
            this.connected = connected;
        }

        @OnOpen
        public void onOpen(Session session) {
            connected.countDown();
            String call = """
                    [2,"evt-1","StatusNotification",\
                    {"connectorId":1,"errorCode":"NoError","status":"Available"}]\
                    """;
            session.getAsyncRemote().sendText(call);
        }
    }
}
