package ch.ecocarsolaire.csms.swap.application.service;

import ch.ecocarsolaire.csms.swap.application.port.dto.SwapFeedbackMessage;
import ch.ecocarsolaire.csms.swap.domain.SwapFeedbackRecord;
import ch.ecocarsolaire.csms.swap.domain.SwapFeedbackRepository;
import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Set;

/**
 * Records driver feedback for swap recommendations (Story 3.1).
 */
@ApplicationScoped
public class DriverFeedback {

    private static final Logger LOG = Logger.getLogger(DriverFeedback.class);
    private static final Set<String> ALLOWED_STATES =
            Set.of("accepted", "dismissed", "completed", "expired");

    private final SwapFeedbackRepository feedbackRepository;
    private final SwapRecommendationRepository recommendationRepository;

    public DriverFeedback(
            SwapFeedbackRepository feedbackRepository,
            SwapRecommendationRepository recommendationRepository) {
        this.feedbackRepository = feedbackRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public void execute(String vehicleId, SwapFeedbackMessage message) {
        if (message.correlationId() == null || message.correlationId().isBlank()) {
            throw new IllegalArgumentException("correlationId is required");
        }
        if (message.state() == null || !ALLOWED_STATES.contains(message.state())) {
            throw new IllegalArgumentException("invalid feedback state: " + message.state());
        }
        if (recommendationRepository.findByCorrelationId(message.correlationId()).isEmpty()) {
            throw new IllegalArgumentException("unknown correlationId: " + message.correlationId());
        }

        feedbackRepository.persist(new SwapFeedbackRecord(
                message.correlationId(),
                vehicleId,
                message.state(),
                Instant.now(),
                message.stationId(),
                message.actualSwapTime()));
        LOG.infof("Recorded swap feedback %s for vehicle %s (correlationId=%s)",
                message.state(), vehicleId, message.correlationId());
    }
}
