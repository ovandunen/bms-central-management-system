package ch.ecocarsolaire;

import ch.ecocarsolaire.csms.location.domain.LocationRepository;
import ch.ecocarsolaire.csms.location.domain.StationLocation;
import ch.ecocarsolaire.csms.station.domain.StationStatus;
import ch.ecocarsolaire.csms.station.domain.ChargingStation;
import ch.ecocarsolaire.csms.station.domain.StationRepository;
import ch.ecocarsolaire.support.PostgisTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies PostGIS {@code ST_DWithin} nearby search returns stations within radius.
 * Requires Docker for the PostGIS devservices test profile.
 */
@QuarkusTest
@QuarkusTestResource(value = PostgisTestResource.class, restrictToAnnotatedClass = true)
@Tag("postgis")
@EnabledIf("com.solarcsms.support.DockerConditions#isDockerAvailable")
class LocationQueryServiceTest {

    @Inject
    LocationRepository locationRepository;

    @Inject
    StationRepository stationRepository;

    @BeforeEach
    @Transactional
    void seedNearbyStations() {
        stationRepository.deleteAll();
        locationRepository.deleteAll();

        ChargingStation berlin = new ChargingStation("CP-BERLIN", 2);
        berlin.updateStatus(StationStatus.AVAILABLE);
        ChargingStation far = new ChargingStation("CP-FAR", 1);
        far.updateStatus(StationStatus.AVAILABLE);
        stationRepository.persist(berlin);
        stationRepository.persist(far);

        locationRepository.persist(new StationLocation(
                "CP-BERLIN", 52.52, 13.405, "Berlin Solar Hub", 120.0));
        locationRepository.persist(new StationLocation(
                "CP-FAR", 48.1351, 11.5820, "Munich Solar Hub", 80.0));
    }

    @Test
    @Transactional
    void findNearbyReturnsStationsWithinRadius() {
        List<StationLocation> nearby = locationRepository.findNearby(52.52, 13.405, 50_000);

        assertFalse(nearby.isEmpty());
        assertTrue(nearby.stream().anyMatch(s -> "CP-BERLIN".equals(s.getStationId())));
        assertFalse(nearby.stream().anyMatch(s -> "CP-FAR".equals(s.getStationId())),
                "Munich station should be outside 50 km of Berlin center");
    }
}
