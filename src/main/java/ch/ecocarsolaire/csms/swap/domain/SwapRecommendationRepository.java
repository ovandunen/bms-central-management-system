package ch.ecocarsolaire.csms.swap.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SwapRecommendationRepository implements PanacheRepositoryBase<SwapRecommendationRecord, UUID> {

    public Optional<SwapRecommendationRecord> findByCorrelationId(String correlationId) {
        return find("correlationId", correlationId).firstResultOptional();
    }
}
