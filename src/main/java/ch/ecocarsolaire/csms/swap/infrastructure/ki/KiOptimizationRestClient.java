package ch.ecocarsolaire.csms.swap.infrastructure.ki;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationResponse;
import ch.ecocarsolaire.csms.swap.application.port.out.KiOptimizationPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Optional;

/**
 * Infrastructure adapter: invokes the external KI service and maps technical failures to {@link Optional#empty()}.
 */
@ApplicationScoped
public class KiOptimizationRestClient implements KiOptimizationPort {

  private final KiOptimizationRemoteApi remoteApi;

  @Inject
  public KiOptimizationRestClient(@RestClient KiOptimizationRemoteApi remoteApi) {
    this.remoteApi = remoteApi;
  }

  @Override
  public Optional<KiOptimizationResponse> requestOptimization(KiOptimizationRequest request) {
    try {
      return Optional.of(remoteApi.evaluate(request));
    } catch (TimeoutException | CircuitBreakerOpenException | ProcessingException | WebApplicationException e) {
      return Optional.empty();
    }
  }
}
