package ch.ecocarsolaire.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * PostGIS container with Flyway migrations enabled (validates {@code db/migration/V*.sql}).
 */
public class PostgisFlywayTestResource implements QuarkusTestResourceLifecycleManager {

  private static final DockerImageName POSTGIS_IMAGE =
      DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> postgis;

  @Override
  public Map<String, String> start() {
    postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE)
        .withDatabaseName("csms")
        .withUsername("csms")
        .withPassword("csms");
    postgis.start();
    return Map.of(
        "quarkus.datasource.devservices.enabled", "false",
        "quarkus.datasource.db-kind", "postgresql",
        "quarkus.datasource.jdbc.url", postgis.getJdbcUrl(),
        "quarkus.datasource.username", postgis.getUsername(),
        "quarkus.datasource.password", postgis.getPassword(),
        "quarkus.hibernate-orm.dialect",
        "org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect",
        "quarkus.hibernate-orm.database.generation", "none",
        "quarkus.flyway.enabled", "true",
        "quarkus.flyway.migrate-at-start", "true");
  }

  @Override
  public void stop() {
    if (postgis != null) {
      postgis.stop();
    }
  }
}
