package ch.ecocarsolaire.csms.swap.infrastructure.ki;

import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationRequest;
import ch.ecocarsolaire.csms.swap.application.port.dto.KiOptimizationResponse;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(KiWireMockTestResource.class)
@Tag("unit")
class KiOptimizationRestClientE2ETest {

  @Inject
  KiOptimizationRestClient restClient;

  @BeforeEach
  void resetStubs() {
    KiWireMockTestResource.server().resetAll();
  }

  @Test
  void evaluateReturnsMappedKiOptimizationResponse() {
    KiWireMockTestResource.server().stubFor(post(urlEqualTo("/api/optimization/evaluate"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                  "score": 0.92,
                  "validFrom": "2026-06-25T14:00:00Z",
                  "validUntil": "2026-06-25T14:30:00Z",
                  "confidence": 0.88,
                  "explanation": "High solar surplus"
                }
                """)));

    KiOptimizationRequest request = sampleRequest();
    Optional<KiOptimizationResponse> result = restClient.requestOptimization(request);

    assertTrue(result.isPresent());
    assertEquals(0.92, result.get().score(), 0.001);
    assertEquals(0.88, result.get().confidence(), 0.001);
    assertEquals("High solar surplus", result.get().explanation());
  }

  @Test
  void evaluateReturnsEmptyOnServerError() {
    KiWireMockTestResource.server().stubFor(post(urlEqualTo("/api/optimization/evaluate"))
        .willReturn(aResponse().withStatus(500)));

    Optional<KiOptimizationResponse> result = restClient.requestOptimization(sampleRequest());

    assertTrue(result.isEmpty());
  }

  private static KiOptimizationRequest sampleRequest() {
    return new KiOptimizationRequest(
        "vehicle-e2e", 18.0, -0.5, 52.52, 13.405, "CP-E2E", 1, 40.0, 12);
  }
}
