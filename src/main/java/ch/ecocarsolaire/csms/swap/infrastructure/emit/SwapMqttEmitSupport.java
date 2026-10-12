package ch.ecocarsolaire.csms.swap.infrastructure.emit;

import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;
import ch.ecocarsolaire.csms.location.infrastructure.DynamicMqttPublisher;
import ch.ecocarsolaire.csms.swap.application.port.out.StationCoordinateLookupPort;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Shared MQTT serialization and publish logic for swap optimization and recommendation topics.
 */
@ApplicationScoped
class SwapMqttEmitSupport {

  private final DynamicMqttPublisher mqttPublisher;
  private final ObjectMapper objectMapper;
  private final StationCoordinateLookupPort stationCoordinates;

  @Inject
  SwapMqttEmitSupport(
      DynamicMqttPublisher mqttPublisher,
      ObjectMapper objectMapper,
      StationCoordinateLookupPort stationCoordinates) {
    this.mqttPublisher = mqttPublisher;
    this.objectMapper = objectMapper;
    this.stationCoordinates = stationCoordinates;
  }

  boolean publish(SwapOptimumDecision decision, String topic, Logger log, String flowLabel) {
    GeoCoordinate coordinates = stationCoordinates.findByStationId(decision.stationId())
        .orElse(null);
    if (coordinates == null) {
      log.warnf(
          "Skipping swap %s for vehicle %s — no coordinates for station %s",
          flowLabel,
          decision.vehicleId(),
          decision.stationId());
      return false;
    }

    SwapRecommendationMqttPayload payload = toPayload(decision, coordinates);
    try {
      byte[] body = objectMapper.writeValueAsBytes(payload);
      mqttPublisher.publish(topic, body);
      return true;
    } catch (JsonProcessingException e) {
      log.errorf(e, "Failed to serialize swap %s for vehicle %s", flowLabel, decision.vehicleId());
      return false;
    }
  }

  private static SwapRecommendationMqttPayload toPayload(
      SwapOptimumDecision decision, GeoCoordinate coordinates) {
    return new SwapRecommendationMqttPayload(
        decision.correlationId(),
        decision.stationId(),
        coordinates.latitude(),
        coordinates.longitude(),
        decision.validFrom(),
        decision.validUntil(),
        decision.reason(),
        decision.confidence(),
        decision.routeEta());
  }
}
