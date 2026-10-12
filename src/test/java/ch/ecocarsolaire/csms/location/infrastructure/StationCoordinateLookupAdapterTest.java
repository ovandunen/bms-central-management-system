package ch.ecocarsolaire.csms.location.infrastructure;

import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;
import ch.ecocarsolaire.csms.location.domain.LocationRepository;
import ch.ecocarsolaire.csms.location.domain.StationLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** JVM unit test: {@link StationCoordinateLookupAdapter} maps persisted station locations to WGS-84. */
@ExtendWith(MockitoExtension.class)
class StationCoordinateLookupAdapterTest {

  @Mock
  LocationRepository locationRepository;

  @InjectMocks
  StationCoordinateLookupAdapter coordinateLookup;

  @Test
  void findByStationId_mapsWgs84FromPersistedLocation() {
    when(locationRepository.findByStationId("CP-0001"))
        .thenReturn(Optional.of(new StationLocation("CP-0001", 46.5197, 6.6323, "Corridor CP-0001", 100.0)));

    Optional<GeoCoordinate> coordinates = coordinateLookup.findByStationId("CP-0001");

    assertTrue(coordinates.isPresent());
    assertEquals(46.5197, coordinates.get().latitude(), 1e-4);
    assertEquals(6.6323, coordinates.get().longitude(), 1e-4);
  }
}
