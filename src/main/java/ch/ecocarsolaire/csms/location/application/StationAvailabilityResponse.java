package ch.ecocarsolaire.csms.location.application;

import java.util.List;

/**
 * MQTT payload for nearby swap stations (schema version 1).
 *
 * @param schemaVersion response schema version
 * @param correlationId links to originating driver pull request
 * @param stations      eligible swap stations within range
 * @param message       optional operator message when {@code stations} is empty
 */
public record StationAvailabilityResponse(
        int schemaVersion,
        String correlationId,
        List<StationLocationDto> stations,
        String message) {

    public static StationAvailabilityResponse withStations(String correlationId, List<StationLocationDto> stations) {
        return new StationAvailabilityResponse(1, correlationId, stations, null);
    }

    public static StationAvailabilityResponse empty(String correlationId, String message) {
        return new StationAvailabilityResponse(1, correlationId, List.of(), message);
    }
}
