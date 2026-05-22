package com.solarcsms;

import com.solarcsms.location.domain.LocationRepository;
import com.solarcsms.location.domain.StationLocation;
import com.solarcsms.station.domain.ChargingStation;
import com.solarcsms.station.domain.StationRepository;
import com.solarcsms.support.DockerConditions;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
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
@TestProfile(LocationQueryServiceTest.PostgisTestProfile.class)
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
        ChargingStation far = new ChargingStation("CP-FAR", 1);
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

    /**
     * Test profile enabling PostGIS devservices.
     */
    public static class PostgisTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "postgis-test";
        }
    }
}
