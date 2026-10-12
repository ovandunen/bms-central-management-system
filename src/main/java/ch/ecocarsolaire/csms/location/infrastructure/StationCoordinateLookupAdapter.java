package ch.ecocarsolaire.csms.location.infrastructure;

import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;
import ch.ecocarsolaire.csms.location.domain.LocationRepository;
import ch.ecocarsolaire.csms.swap.application.port.out.StationCoordinateLookupPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

/**
 * Adapter: {@link LocationRepository} → {@link StationCoordinateLookupPort}.
 */
@ApplicationScoped
public class StationCoordinateLookupAdapter implements StationCoordinateLookupPort {

  private final LocationRepository locationRepository;

  @Inject
  public StationCoordinateLookupAdapter(LocationRepository locationRepository) {
    this.locationRepository = locationRepository;
  }

  @Override
  @Transactional
  public Optional<GeoCoordinate> findByStationId(String stationId) {
    return locationRepository.findByStationId(stationId)
        .map(location -> new GeoCoordinate(location.getLatitude(), location.getLongitude()));
  }
}
