package ch.ecocarsolaire.csms.location.application;

import ch.ecocarsolaire.csms.location.domain.StationLocation;
import ch.ecocarsolaire.csms.station.domain.ChargingStation;
import ch.ecocarsolaire.csms.station.domain.StationStatus;

/**
 * MQTT payload record for a single nearby station (chargeable = {@link StationStatus#AVAILABLE}).
 *
 * @param stationId        OCPP charge point id
 * @param displayName      human-readable name (may be null)
 * @param streetAddress    optional street line for map/list
 * @param city             optional city
 * @param latitude         WGS-84 latitude
 * @param longitude        WGS-84 longitude
 * @param solarCapacityKw  installed solar capacity in kW
 * @param status           operational status name (e.g. AVAILABLE)
 */
public record StationLocationDto(
        String stationId,
        String displayName,
        String streetAddress,
        String city,
        double latitude,
        double longitude,
        double solarCapacityKw,
        String status,
        Integer chargedPacksReady,
        Boolean swapBayFree,
        Double solarSurplusKw,
        Double distanceKm,
        Integer estimatedWaitMin) {

    /**
     * Maps a domain aggregate to a DTO suitable for JSON serialization.
     *
     * @param location station location aggregate
     * @param status   operational status from {@link ChargingStation}
     * @return DTO instance
     */
    public static StationLocationDto from(StationLocation location, StationStatus status) {
        return from(location, status, null, null);
    }

    /**
     * Maps location, status, and optional swap telemetry into a pull-response DTO.
     */
    public static StationLocationDto from(
            StationLocation location,
            StationStatus status,
            ch.ecocarsolaire.csms.station.domain.StationSwapState swapState,
            Double distanceKm) {
        Integer chargedPacks = swapState != null ? swapState.getChargedPacksReady() : null;
        Boolean bayFree = swapState != null ? swapState.isSwapBayFree() : null;
        Double solarSurplus = swapState != null ? swapState.getSolarSurplusKw() : null;
        return new StationLocationDto(
                location.getStationId(),
                location.getDisplayName(),
                location.getStreetAddress(),
                location.getCity(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSolarCapacityKw(),
                status.name(),
                chargedPacks,
                bayFree,
                solarSurplus,
                distanceKm,
                chargedPacks != null && chargedPacks > 0 ? 5 : null);
    }
}
