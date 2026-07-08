package ch.ecocarsolaire.csms.swap.infrastructure.ki;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * Starts WireMock for KI REST client E2E tests and points {@code ki-service} RestClient at it.
 */
public class KiWireMockTestResource implements QuarkusTestResourceLifecycleManager {

  private static WireMockServer server;

  public static WireMockServer server() {
    return server;
  }

  @Override
  public Map<String, String> start() {
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    String baseUrl = server.baseUrl();
    return Map.of(
        "ki.service.url", baseUrl,
        "quarkus.rest-client.ki-service.url", baseUrl);
  }

  @Override
  public void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }
}
