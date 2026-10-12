package ch.ecocarsolaire.csms.swap.application.port.out;

import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;

/**
 * Outbound port for low-battery swap recommendations to the vehicle (UC-06).
 * MQTT topic: {@code vehicles/{vehicleId}/swap/recommendation}.
 */
public interface SwapRecommendationEmitterPort {

  /**
   * @param decision low-battery swap recommendation for the driver
   */
  void emit(SwapOptimumDecision decision);
}
