package ch.ecocarsolaire.csms.swap.infrastructure.ingest;

import ch.ecocarsolaire.csms.swap.application.port.dto.VehicleTelemetryMessage;
import ch.ecocarsolaire.csms.swap.application.service.OptimizeSwapMoment;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consumes vehicle telemetry from MQTT and triggers swap optimization (Story 1.2 / UC-02).
 */
@ApplicationScoped
public class VehicleTelemetryMqttHandler {

  private static final Logger LOG = Logger.getLogger(VehicleTelemetryMqttHandler.class);
  private static final Pattern TOPIC_PATTERN = Pattern.compile("^vehicles/([^/]+)/telemetry$");

  private final OptimizeSwapMoment optimizeSwapMomentUseCase;
  private final ObjectMapper objectMapper;

  public VehicleTelemetryMqttHandler(
      OptimizeSwapMoment optimizeSwapMomentUseCase,
      ObjectMapper objectMapper) {
    this.optimizeSwapMomentUseCase = optimizeSwapMomentUseCase;
    this.objectMapper = objectMapper;
  }

  /**
   * Handles inbound MQTT messages on {@code vehicles/+/telemetry}.
   *
   * @param msg reactive messaging envelope with raw JSON payload
   * @return completion stage acknowledging the message
   */
  @Incoming("vehicle-telemetry")
  @Blocking
  public CompletionStage<Void> handle(Message<byte[]> msg) {
    try {
      Optional<String> vehicleIdFromTopic = msg.getMetadata(MqttMessage.class)
          .map(MqttMessage::getTopic)
          .flatMap(topic -> {
            Matcher matcher = TOPIC_PATTERN.matcher(topic);
            return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
          });

      VehicleTelemetryMessage telemetry =
          objectMapper.readValue(msg.getPayload(), VehicleTelemetryMessage.class);
      String vehicleId = vehicleIdFromTopic.orElse(telemetry.vehicleId());
      if (vehicleId == null || vehicleId.isBlank()) {
        throw new IllegalArgumentException("vehicleId missing in topic and payload");
      }

      optimizeSwapMomentUseCase.execute(telemetry.toOptimizationRequest(vehicleId));
      LOG.debugf("Processed vehicle telemetry for %s", vehicleId);
    } catch (Exception e) {
      LOG.errorf(e, "Invalid vehicle telemetry message");
    }
    return msg.ack();
  }
}
