package ch.ecocarsolaire.csms.swap.application.port.out;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationResponse;

import java.util.Optional;

/**
 * Outbound port for invoking external KI pattern-recognition optimization (UC-04).
 */
public interface KiOptimizationPort {

    /**
     * Requests an optimum swap window score from the external KI service.
     *
     * @param request optimization feature vector
     * @return KI response when available; empty when the service is unavailable or circuit is open
     */
    Optional<KiOptimizationResponse> requestOptimization(KiOptimizationRequest request);
}
