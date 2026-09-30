package ch.ecocarsolaire.csms.swap.application.service;

import ch.ecocarsolaire.csms.swap.domain.SwapFeedbackRecord;
import ch.ecocarsolaire.csms.swap.domain.SwapFeedbackRepository;
import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRecord;
import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRepository;
import ch.ecocarsolaire.csms.swap.infrastructure.ki.KiFeedbackRemoteApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports unprocessed swap feedback to the KI retraining endpoint (Story 3.2).
 */
@ApplicationScoped
public class SwapFeedback {

    private static final Logger LOG = Logger.getLogger(SwapFeedback.class);

    private final SwapFeedbackRepository feedbackRepository;
    private final SwapRecommendationRepository recommendationRepository;
    private final KiFeedbackRemoteApi kiFeedbackApi;

    public SwapFeedback(
            SwapFeedbackRepository feedbackRepository,
            SwapRecommendationRepository recommendationRepository,
            @RestClient KiFeedbackRemoteApi kiFeedbackApi) {
        this.feedbackRepository = feedbackRepository;
        this.recommendationRepository = recommendationRepository;
        this.kiFeedbackApi = kiFeedbackApi;
    }

    @Transactional
    public int execute() {
        List<SwapFeedbackRecord> pending = feedbackRepository.findUnprocessed();
        if (pending.isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> batch = pending.stream().map(this::toExportRecord).toList();
        kiFeedbackApi.submitFeedbackBatch(batch);

        pending.forEach(SwapFeedbackRecord::markProcessed);
        LOG.infof("Exported %d swap feedback records to KI", pending.size());
        return pending.size();
    }

    private Map<String, Object> toExportRecord(SwapFeedbackRecord feedback) {
        SwapRecommendationRecord recommendation = recommendationRepository
                .findByCorrelationId(feedback.getCorrelationId())
                .orElseThrow();

        Map<String, Object> record = new HashMap<>();
        record.put("correlationId", feedback.getCorrelationId());
        record.put("vehicleId", recommendation.getVehicleId());
        record.put("stationId", recommendation.getStationId());
        record.put("feedbackState", feedback.getState());
        record.put("confidence", recommendation.getConfidence());
        record.put("reason", recommendation.getReason());
        return record;
    }
}
