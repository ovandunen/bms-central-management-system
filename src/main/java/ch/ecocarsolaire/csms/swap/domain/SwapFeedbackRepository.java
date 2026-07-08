package ch.ecocarsolaire.csms.swap.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SwapFeedbackRepository implements PanacheRepositoryBase<SwapFeedbackRecord, UUID> {

    public List<SwapFeedbackRecord> findUnprocessed() {
        return list("processed", false);
    }
}
