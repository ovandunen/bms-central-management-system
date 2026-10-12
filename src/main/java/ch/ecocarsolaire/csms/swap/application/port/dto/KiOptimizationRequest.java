package ch.ecocarsolaire.csms.swap.application.port.dto;

/**
 * Request payload for external KI pattern-recognition optimization.
 *
 * @param vehicleId         vehicle identifier
 * @param currentSoc        state of charge (0.0–1.0 or percent per upstream convention)
 * @param socDelta          change in SoC over the observation window
 * @param latitude          WGS-84 latitude
 * @param longitude         WGS-84 longitude
 * @param stationId         target swap station identifier
 * @param stationInventory  charged packs ready at station
 * @param solarSurplusKw    available solar surplus in kW
 * @param etaMinutes        estimated minutes until vehicle reaches station
 */
public record KiOptimizationRequest(
        String vehicleId,
        double currentSoc,
        double socDelta,
        double latitude,
        double longitude,
        String stationId,
        int stationInventory,
        double solarSurplusKw,
        int etaMinutes) {
}
