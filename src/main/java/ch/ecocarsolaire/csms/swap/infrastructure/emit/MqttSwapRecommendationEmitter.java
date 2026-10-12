package ch.ecocarsolaire.csms.swap.infrastructure.emit;

import ch.ecocarsolaire.csms.swap.application.port.out.SwapRecommendationEmitterPort;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.logging.Logger;

/**
 * Publishes low-battery swap recommendations to {@code vehicles/{vehicleId}/swap/recommendation}.
 */
@ApplicationScoped
@Priority(1)
public class MqttSwapRecommendationEmitter implements SwapRecommendationEmitterPort {

  private static final Logger LOG = Logger.getLogger(MqttSwapRecommendationEmitter.class);
  private static final String METRIC_NAME = "swap.recommendation.sent";

  private final SwapMqttEmitSupport emitSupport;
  private final Counter sentCounter;

  @Inject
  public MqttSwapRecommendationEmitter(
      SwapMqttEmitSupport emitSupport,
      @RegistryType(type = MetricRegistry.Type.APPLICATION) MetricRegistry metricRegistry) {
    this.emitSupport = emitSupport;
    this.sentCounter = metricRegistry.counter(METRIC_NAME);
  }

  @Override
  public void emit(SwapOptimumDecision decision) {
    String topic = "vehicles/" + decision.vehicleId() + "/swap/recommendation";
    if (emitSupport.publish(decision, topic, LOG, "recommendation")) {
      sentCounter.inc();
    }
  }
}
