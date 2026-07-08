package ch.ecocarsolaire.csms.swap.application.service;

import ch.ecocarsolaire.csms.swap.application.port.dto.SwapFeedbackMessage;
import ch.ecocarsolaire.csms.swap.domain.SwapFeedbackRecord;
import ch.ecocarsolaire.csms.swap.domain.SwapFeedbackRepository;
import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRecord;
import ch.ecocarsolaire.csms.swap.domain.SwapRecommendationRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@Tag("unit")
class RecordSwapFeedbackUseCaseTest {

    @Inject
    RecordSwapFeedbackUseCase useCase;

    @Inject
    SwapRecommendationRepository recommendationRepository;

    @Inject
    SwapFeedbackRepository feedbackRepository;

    @BeforeEach
    @Transactional
    void seedRecommendation() {
        feedbackRepository.deleteAll();
        recommendationRepository.deleteAll();
        recommendationRepository.persist(new SwapRecommendationRecord(
                "rec-001",
                "EV-001",
                "Station-A",
                Instant.now(),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15),
                0.9,
                "test"));
    }

    @Test
    @Transactional
    void recordSwapFeedback_validAcceptedState_persists() {
        useCase.execute("EV-001", new SwapFeedbackMessage("EV-001", "rec-001", "accepted", null, null));

        assertEquals(1, feedbackRepository.count());
        SwapFeedbackRecord record = feedbackRepository.findAll().firstResult();
        assertEquals("accepted", record.getState());
    }

    @Test
    @Transactional
    void recordSwapFeedback_unknownCorrelationId_rejects() {
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute("EV-001", new SwapFeedbackMessage("EV-001", "missing", "accepted", null, null)));
    }
}
