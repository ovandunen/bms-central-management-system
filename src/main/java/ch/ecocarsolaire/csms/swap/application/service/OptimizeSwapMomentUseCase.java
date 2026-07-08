package ch.ecocarsolaire.csms.swap.application.service;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationResponse;
import ch.ecocarsolaire.csms.swap.application.port.out.KiOptimizationPort;
import ch.ecocarsolaire.csms.swap.application.port.out.SwapOptimizationEmitterPort;
import ch.ecocarsolaire.csms.swap.application.port.out.SwapRecommendationEmitterPort;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates KI pattern recognition and rule-based scoring to decide the optimum battery swap moment (UC-05).
 */
@ApplicationScoped
public class OptimizeSwapMomentUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizeSwapMomentUseCase.class);
  private static final double KI_BLEND_WEIGHT = 0.5;
  private static final double RULE_BLEND_WEIGHT = 0.5;
  private static final double DECISION_THRESHOLD = 0.5;
  private static final double RULE_SCORE_HIGH = 0.8;
  private static final double RULE_SCORE_LOW = 0.0;
  private static final double SOC_THRESHOLD_PERCENT = 20.0;
  private static final double KI_MIN_CONFIDENCE = 0.7;

  private final KiOptimizationPort kiPort;
  private final SwapOptimizationEmitterPort optimizationEmitter;
  private final SwapRecommendationEmitterPort recommendationEmitter;
  private final Event<SwapOptimumDecision> swapDecisionEvent;
  private final Map<String, Instant> lastDecisionByVehicle = new ConcurrentHashMap<>();

  @ConfigProperty(name = "app.swap.cooldown-seconds", defaultValue = "900")
  long cooldownSeconds;

  public OptimizeSwapMomentUseCase(
      KiOptimizationPort kiPort,
      SwapOptimizationEmitterPort optimizationEmitter,
      SwapRecommendationEmitterPort recommendationEmitter,
      Event<SwapOptimumDecision> swapDecisionEvent) {
    this.kiPort = kiPort;
    this.optimizationEmitter = optimizationEmitter;
    this.recommendationEmitter = recommendationEmitter;
    this.swapDecisionEvent = swapDecisionEvent;
  }

  /**
   * Evaluates whether to recommend a battery swap for the given optimization request.
   *
   * @param request vehicle and station feature vector
   */
  public void execute(KiOptimizationRequest request) {
    if (isWithinCooldown(request.vehicleId())) {
      LOG.debug("Swap decision skipped for vehicle {} — within {}s cooldown", request.vehicleId(), cooldownSeconds);
      return;
    }

    Optional<KiOptimizationResponse> kiResponse = kiPort.requestOptimization(request);
    if (kiResponse.isPresent() && kiResponse.get().confidence() < KI_MIN_CONFIDENCE) {
      LOG.info("Swap decision suppressed for vehicle {} — KI confidence {} below {}",
          request.vehicleId(), kiResponse.get().confidence(), KI_MIN_CONFIDENCE);
      return;
    }
    double ruleScore = evaluateRuleScore(request);
    double finalScore = kiResponse
        .map(ki -> (ruleScore * RULE_BLEND_WEIGHT) + (ki.score() * KI_BLEND_WEIGHT))
        .orElse(ruleScore);

    if (finalScore <= DECISION_THRESHOLD) {
      LOG.debug("Swap decision skipped for vehicle {} — finalScore {} below threshold {}",
          request.vehicleId(), finalScore, DECISION_THRESHOLD);
      return;
    }

    SwapOptimumDecision decision = buildDecision(request, kiResponse, ruleScore, finalScore);
    lastDecisionByVehicle.put(request.vehicleId(), Instant.now());
    optimizationEmitter.emit(decision);
    if (isLowBattery(request)) {
      recommendationEmitter.emit(decision);
    }
    swapDecisionEvent.fire(decision);
    LOG.info("Swap optimum decision for vehicle {} at station {} — finalScore={}, reason={}",
        decision.vehicleId(), decision.stationId(), decision.finalScore(), decision.reason());
  }

  private boolean isWithinCooldown(String vehicleId) {
    Instant lastDecision = lastDecisionByVehicle.get(vehicleId);
    if (lastDecision == null) {
      return false;
    }
    return Instant.now().minusSeconds(cooldownSeconds).isBefore(lastDecision);
  }

  private double evaluateRuleScore(KiOptimizationRequest request) {
    if (isLowBattery(request)) {
      return RULE_SCORE_HIGH;
    }
    return RULE_SCORE_LOW;
  }

  private static boolean isLowBattery(KiOptimizationRequest request) {
    return request.currentSoc() < SOC_THRESHOLD_PERCENT && request.stationInventory() > 0;
  }

  private SwapOptimumDecision buildDecision(
      KiOptimizationRequest request,
      Optional<KiOptimizationResponse> kiResponse,
      double ruleScore,
      double finalScore) {
    OffsetDateTime validFrom = kiResponse.map(KiOptimizationResponse::validFrom)
        .orElse(OffsetDateTime.now(ZoneOffset.UTC));
    OffsetDateTime validUntil = kiResponse.map(KiOptimizationResponse::validUntil)
        .orElse(validFrom.plusMinutes(15));
    double confidence = kiResponse.map(KiOptimizationResponse::confidence).orElse(ruleScore);
    String reason = kiResponse
        .map(ki -> "KI+rule blend (kiScore=" + ki.score() + ", ruleScore=" + ruleScore + ")")
        .orElse("rule-only (KI unavailable, ruleScore=" + ruleScore + ")");

    return new SwapOptimumDecision(
        UUID.randomUUID().toString(),
        request.vehicleId(),
        request.stationId(),
        validFrom,
        validUntil,
        confidence,
        finalScore,
        reason,
        request.etaMinutes());
  }
}
