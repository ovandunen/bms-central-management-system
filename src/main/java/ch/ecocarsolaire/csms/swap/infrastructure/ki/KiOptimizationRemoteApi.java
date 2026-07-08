package ch.ecocarsolaire.csms.swap.infrastructure.ki;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * JAX-RS contract for the external KI optimization HTTP API.
 * Fault tolerance is applied at this boundary; failures are translated in {@link KiOptimizationRestClient}.
 */
@RegisterRestClient(configKey = "ki-service")
@Path("/api/optimization")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface KiOptimizationRemoteApi {

  @POST
  @Path("/evaluate")
  @Timeout(2000)
  @Retry(maxRetries = 3)
  @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
  KiOptimizationResponse evaluate(KiOptimizationRequest request);
}
