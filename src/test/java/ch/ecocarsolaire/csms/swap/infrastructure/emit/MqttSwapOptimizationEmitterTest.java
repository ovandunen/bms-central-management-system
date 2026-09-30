package ch.ecocarsolaire.csms.swap.infrastructure.emit;

import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;
import ch.ecocarsolaire.csms.swap.application.port.out.StationCoordinateLookupPort;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.ecocarsolaire.csms.location.infrastructure.DynamicMqttPublisher;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@Tag("unit")
class MqttSwapOptimizationEmitterTest {

  @InjectMock
  DynamicMqttPublisher mqttPublisher;

  @InjectMock
  StationCoordinateLookupPort stationCoordinates;

  @Inject
  MqttSwapOptimizationEmitter emitter;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  void resetMocks() {
    reset(mqttPublisher, stationCoordinates);
    when(stationCoordinates.findByStationId("CP-SWAP-01"))
        .thenReturn(Optional.of(new GeoCoordinate(52.52, 13.405)));
  }

  @Test
  void emitPublishesToOptimizationTopicWithCoordinates() throws Exception {
    SwapOptimumDecision decision = sampleDecision();

    emitter.emit(decision);

    ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(mqttPublisher, times(1)).publish(topicCaptor.capture(), payloadCaptor.capture());

    assertEquals("swap/optimization/vehicle-uc06", topicCaptor.getValue());

    SwapRecommendationMqttPayload payload =
        objectMapper.readValue(payloadCaptor.getValue(), SwapRecommendationMqttPayload.class);
    assertEquals(52.52, payload.latitude(), 1e-4);
    assertEquals(13.405, payload.longitude(), 1e-4);
    assertEquals(decision.stationId(), payload.stationId());
  }

  @Test
  void emitSkipsPublishWhenStationCoordinatesMissing() {
    when(stationCoordinates.findByStationId(anyString())).thenReturn(Optional.empty());

    emitter.emit(sampleDecision());

    verify(mqttPublisher, never()).publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  private static SwapOptimumDecision sampleDecision() {
    OffsetDateTime from = OffsetDateTime.of(2026, 6, 25, 14, 0, 0, 0, ZoneOffset.UTC);
    return new SwapOptimumDecision(
        "rec-uc06",
        "vehicle-uc06",
        "CP-SWAP-01",
        from,
        from.plusMinutes(30),
        0.88,
        0.85,
        "KI+rule blend (kiScore=0.9, ruleScore=0.8)",
        12);
  }
}
