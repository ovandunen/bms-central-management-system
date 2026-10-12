package ch.ecocarsolaire.csms.location.application;

import ch.ecocarsolaire.csms.location.domain.GeoCoordinate;

/**
 * CDI event representing a driver request for nearby charging stations.
 *
 * @param driverId       external driver identifier (MQTT topic segment)
 * @param coordinate     search center
 * @param radiusMeters   search radius in metres
 * @param correlationId  id linking pull request to response
 */
public record NearbyStationQuery(
        String driverId,
        GeoCoordinate coordinate,
        double radiusMeters,
        String correlationId) {
}
