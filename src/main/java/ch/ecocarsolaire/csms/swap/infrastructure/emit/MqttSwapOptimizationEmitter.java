package ch.ecocarsolaire.csms.swap.infrastructure.emit;

import ch.ecocarsolaire.csms.swap.application.port.out.SwapOptimizationEmitterPort;
import ch.ecocarsolaire.csms.swap.domain.event.SwapOptimumDecision;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.logging.Logger;

/**
 * Publishes KI/rule optimization outcomes to {@code swap/optimization/{vehicleId}}.
 */
@ApplicationScoped
@Priority(1)
public class MqttSwapOptimizationEmitter implements SwapOptimizationEmitterPort {

  private static final Logger LOG = Logger.getLogger(MqttSwapOptimizationEmitter.class);
  private static final String METRIC_NAME = "swap.optimization.sent";

  private final SwapMqttEmitSupport emitSupport;
  private final Counter sentCounter;

  @Inject
  public MqttSwapOptimizationEmitter(
      SwapMqttEmitSupport emitSupport,
      @RegistryType(type = MetricRegistry.Type.APPLICATION) MetricRegistry metricRegistry) {
    this.emitSupport = emitSupport;
    this.sentCounter = metricRegistry.counter(METRIC_NAME);
  }

  @Override
  public void emit(SwapOptimumDecision decision) {
    String topic = "swap/optimization/" + decision.vehicleId();
    if (emitSupport.publish(decision, topic, LOG, "optimization")) {
      sentCounter.inc();
    }
  }
}
