package ch.ecocarsolaire.csms.swap.application.port.out;

import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;

import java.util.Optional;

/**
 * Outbound port: resolve WGS-84 coordinates for a charge point / station id.
 */
public interface StationCoordinateLookupPort {

  /**
   * @param stationId OCPP charge point identifier ({@code charging_station.station_id})
   * @return coordinates when {@code station_location} exists for the id
   */
  Optional<GeoCoordinate> findByStationId(String stationId);
}
