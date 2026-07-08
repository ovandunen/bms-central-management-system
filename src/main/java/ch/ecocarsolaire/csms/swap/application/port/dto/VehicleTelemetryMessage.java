package ch.ecocarsolaire.csms.swap.application.port.dto;

/**
 * Vehicle telemetry published by BMS monitoring on {@code vehicles/{vehicleId}/telemetry} (Story 1.2).
 *
 * @param vehicleId         vehicle identifier (optional when taken from MQTT topic)
 * @param currentSoc        state of charge in percent (0–100)
 * @param socDelta          change in SoC over the observation window
 * @param latitude          WGS-84 latitude
 * @param longitude         WGS-84 longitude
 * @param stationId         nearest / target swap station identifier
 * @param stationInventory  charged packs ready at station
 * @param solarSurplusKw    available solar surplus in kW
 * @param etaMinutes        estimated minutes until vehicle reaches station
 */
public record VehicleTelemetryMessage(
        String vehicleId,
        double currentSoc,
        double socDelta,
        double latitude,
        double longitude,
        String stationId,
        int stationInventory,
        double solarSurplusKw,
        int etaMinutes) {

  public KiOptimizationRequest toOptimizationRequest(String resolvedVehicleId) {
    return new KiOptimizationRequest(
        resolvedVehicleId,
        currentSoc,
        socDelta,
        latitude,
        longitude,
        stationId,
        stationInventory,
        solarSurplusKw,
        etaMinutes);
  }
}
