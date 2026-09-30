package ch.ecocarsolaire.csms.swap.infrastructure.ingest;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.VehicleTelemetryMessage;
import ch.ecocarsolaire.csms.swap.application.service.OptimizeSwapMoment;
import ch.ecocarsolaire.support.InMemoryMessagingTestResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
@Tag("unit")
class VehicleTelemetryMqttHandlerTest {

  @Inject
  @Any
  InMemoryConnector connector;

  @Inject
  ObjectMapper objectMapper;

  @InjectMock
  OptimizeSwapMoment optimizeSwapMomentUseCase;

  @Test
  void telemetryOnMqttTopicTriggersSwapOptimization() throws Exception {
    InMemorySource<Message<byte[]>> source = connector.source("vehicle-telemetry");
    VehicleTelemetryMessage telemetry = new VehicleTelemetryMessage(
        null,
        18.0,
        -0.5,
        52.52,
        13.405,
        "CP-E2E",
        2,
        40.0,
        12);
    byte[] payload = objectMapper.writeValueAsBytes(telemetry);

    source.send(Message.of(payload)
        .addMetadata(MqttMessage.of("vehicles/veh-001/telemetry", payload)));

    verify(optimizeSwapMomentUseCase, timeout(2000).times(1))
        .execute(argThat((KiOptimizationRequest request) ->
            "veh-001".equals(request.vehicleId())
                && request.currentSoc() == 18.0
                && "CP-E2E".equals(request.stationId())));
  }
}
