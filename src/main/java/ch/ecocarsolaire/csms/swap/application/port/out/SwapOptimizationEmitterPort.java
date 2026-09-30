package ch.ecocarsolaire.csms.swap.application.port.out;

import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;

/**
 * Outbound port for publishing KI/rule optimization outcomes (UC-04 / UC-05).
 * MQTT topic: {@code swap/optimization/{vehicleId}}.
 */
public interface SwapOptimizationEmitterPort {

  /**
   * @param decision computed swap optimization outcome
   */
  void emit(SwapOptimumDecision decision);
}
