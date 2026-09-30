package ch.ecocarsolaire.csms.swap.infrastructure.ingest;

import ch.ecocarsolaire.csms.swap.application.port.dto.SwapFeedbackMessage;
import ch.ecocarsolaire.csms.swap.application.service.DriverFeedback;
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
 * Consumes driver swap feedback from MQTT (Story 3.1).
 */
@ApplicationScoped
public class SwapFeedbackMqttHandler {

    private static final Logger LOG = Logger.getLogger(SwapFeedbackMqttHandler.class);
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^vehicles/([^/]+)/swap/feedback$");

    private final DriverFeedback recordSwapFeedbackUseCase;
    private final ObjectMapper objectMapper;

    public SwapFeedbackMqttHandler(
            DriverFeedback recordSwapFeedbackUseCase,
            ObjectMapper objectMapper) {
        this.recordSwapFeedbackUseCase = recordSwapFeedbackUseCase;
        this.objectMapper = objectMapper;
    }

    @Incoming("swap-feedback")
    @Blocking
    public CompletionStage<Void> handle(Message<byte[]> msg) {
        try {
            Optional<String> vehicleIdFromTopic = msg.getMetadata(MqttMessage.class)
                    .map(MqttMessage::getTopic)
                    .flatMap(topic -> {
                        Matcher matcher = TOPIC_PATTERN.matcher(topic);
                        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
                    });

            SwapFeedbackMessage feedback =
                    objectMapper.readValue(msg.getPayload(), SwapFeedbackMessage.class);
            String vehicleId = vehicleIdFromTopic.orElse(feedback.vehicleId());
            if (vehicleId == null || vehicleId.isBlank()) {
                throw new IllegalArgumentException("vehicleId missing in topic and payload");
            }
            recordSwapFeedbackUseCase.execute(vehicleId, feedback);
        } catch (Exception e) {
            LOG.errorf(e, "Invalid swap feedback message");
        }
        return msg.ack();
    }
}
