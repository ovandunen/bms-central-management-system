package ch.ecocarsolaire.csms.swap.application.service;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationResponse;
import ch.ecocarsolaire.csms.swap.application.port.out.KiOptimizationPort;
import ch.ecocarsolaire.csms.swap.application.port.out.SwapOptimizationEmitterPort;
import ch.ecocarsolaire.csms.swap.application.port.out.SwapRecommendationEmitterPort;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@Tag("unit")
class OptimizeSwapMomentUseCaseTest {

  @InjectMock
  KiOptimizationPort kiPort;

  @InjectMock
  SwapOptimizationEmitterPort optimizationEmitter;

  @InjectMock
  SwapRecommendationEmitterPort recommendationEmitter;

  @Inject
  OptimizeSwapMoment useCase;

  @BeforeEach
  void resetMocks() {
    org.mockito.Mockito.reset(kiPort, optimizationEmitter, recommendationEmitter);
  }

  @Test
  void happyPathBlendsKiAndRuleScores() {
    when(kiPort.requestOptimization(any())).thenReturn(Optional.of(kiResponse(0.9)));

    useCase.execute(lowSocRequest("vehicle-happy"));

    ArgumentCaptor<SwapOptimumDecision> captor = ArgumentCaptor.forClass(SwapOptimumDecision.class);
    verify(optimizationEmitter, times(1)).emit(captor.capture());
    verify(recommendationEmitter, times(1)).emit(captor.getValue());
    SwapOptimumDecision decision = captor.getValue();
    assertEquals(0.85, decision.finalScore(), 0.001);
    assertEquals(10, decision.routeEta());
    assertTrue(decision.reason().contains("KI+rule"));
  }

  @Test
  void cooldownSuppressesSecondDecisionWithinFifteenMinutes() {
    when(kiPort.requestOptimization(any())).thenReturn(Optional.of(kiResponse(0.9)));
    KiOptimizationRequest request = lowSocRequest("vehicle-cooldown");

    useCase.execute(request);
    useCase.execute(request);

    verify(optimizationEmitter, times(1)).emit(any(SwapOptimumDecision.class));
    verify(recommendationEmitter, times(1)).emit(any(SwapOptimumDecision.class));
  }

  @Test
  void kiUnavailableFallsBackToRuleOnly() {
    when(kiPort.requestOptimization(any())).thenReturn(Optional.empty());

    useCase.execute(lowSocRequest("vehicle-fallback"));

    ArgumentCaptor<SwapOptimumDecision> captor = ArgumentCaptor.forClass(SwapOptimumDecision.class);
    verify(optimizationEmitter, times(1)).emit(captor.capture());
    verify(recommendationEmitter, times(1)).emit(captor.getValue());
    assertTrue(captor.getValue().reason().contains("rule-only"));
    assertEquals(0.8, captor.getValue().finalScore(), 0.001);
  }

  @Test
  void thresholdNotMetDoesNotEmit() {
    when(kiPort.requestOptimization(any())).thenReturn(Optional.empty());

    useCase.execute(highSocRequest("vehicle-threshold"));

    verify(optimizationEmitter, never()).emit(any(SwapOptimumDecision.class));
    verify(recommendationEmitter, never()).emit(any(SwapOptimumDecision.class));
  }

  @Test
  void kiLowConfidenceSuppressesRecommendation() {
    when(kiPort.requestOptimization(any())).thenReturn(Optional.of(kiResponse(0.9, 0.55)));

    useCase.execute(lowSocRequest("vehicle-lowconf"));

    verify(optimizationEmitter, never()).emit(any(SwapOptimumDecision.class));
    verify(recommendationEmitter, never()).emit(any(SwapOptimumDecision.class));
  }

  private static KiOptimizationRequest lowSocRequest(String vehicleId) {
    return new KiOptimizationRequest(
        vehicleId, 15.0, -1.0, 52.52, 13.405, "CP-001", 2, 50.0, 10);
  }

  private static KiOptimizationRequest highSocRequest(String vehicleId) {
    return new KiOptimizationRequest(
        vehicleId, 80.0, 0.0, 52.52, 13.405, "CP-001", 2, 50.0, 10);
  }

  private static KiOptimizationResponse kiResponse(double score) {
    return kiResponse(score, 0.95);
  }

  private static KiOptimizationResponse kiResponse(double score, double confidence) {
    OffsetDateTime from = OffsetDateTime.of(2026, 6, 25, 14, 0, 0, 0, ZoneOffset.UTC);
    return new KiOptimizationResponse(score, from, from.plusMinutes(30), confidence, "KI recommends swap window");
  }
}
