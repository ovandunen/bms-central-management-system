package ch.ecocarsolaire;

import ch.ecocarsolaire.csms.location.domain.LocationRepository;
import ch.ecocarsolaire.csms.location.domain.StationLocation;
import ch.ecocarsolaire.support.PostgisFlywayTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JVM integration test: Flyway V5 seeds fleet station coordinates retrievable via PostGIS.
 */
@QuarkusTest
@QuarkusTestResource(value = PostgisFlywayTestResource.class, restrictToAnnotatedClass = true)
@Tag("postgis")
@Tag("integration")
@EnabledIf("com.solarcsms.support.DockerConditions#isDockerAvailable")
class FlywayV5StationCoordinateSeedTest {

  @Inject
  LocationRepository locationRepository;

  @Test
  @Transactional
  void zurichDepotHasSeededCoordinates() {
    Optional<StationLocation> location = locationRepository.findByStationId("ZUR-DEPOT-01");
    assertTrue(location.isPresent(), "V5 migration should seed ZUR-DEPOT-01");
    assertEquals(47.3769, location.get().getLatitude(), 1e-4);
    assertEquals(8.5417, location.get().getLongitude(), 1e-4);
  }

  @Test
  @Transactional
  void dakarStationHasSeededCoordinates() {
    Optional<StationLocation> location = locationRepository.findByStationId("DKR-STATION-03");
    assertTrue(location.isPresent(), "V5 migration should seed DKR-STATION-03");
    assertEquals(14.7167, location.get().getLatitude(), 1e-4);
    assertEquals(-17.4677, location.get().getLongitude(), 1e-4);
  }
}
