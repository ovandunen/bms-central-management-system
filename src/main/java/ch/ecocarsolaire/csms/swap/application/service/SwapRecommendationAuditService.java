package ch.ecocarsolaire.csms.swap.application.service;

import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRecord;
import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRepository;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Persists swap recommendations for audit and feedback correlation (Story 3.1).
 */
@ApplicationScoped
public class SwapRecommendationAuditService {

    private static final Logger LOG = Logger.getLogger(SwapRecommendationAuditService.class);

    private final SwapRecommendationRepository repository;

    public SwapRecommendationAuditService(SwapRecommendationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void onSwapDecision(@Observes SwapOptimumDecision decision) {
        repository.persist(new SwapRecommendationRecord(
                decision.correlationId(),
                decision.vehicleId(),
                decision.stationId(),
                Instant.now(),
                decision.validFrom(),
                decision.validUntil(),
                decision.confidence(),
                decision.reason()));
        LOG.debugf("Audit stored recommendation %s for vehicle %s",
                decision.correlationId(), decision.vehicleId());
    }
}
