package ch.ecocarsolaire.csms.location.domain;

import ch.ecocarsolaire.csms.station.domain.StationSwapState;

import java.time.Duration;
import java.time.Instant;

/**
 * Domain policy for swap-station eligibility in pull-flow responses.
 */
public final class SwapStationEligibilityPolicy {

    private SwapStationEligibilityPolicy() {
    }

    /**
     * @param swapState           station swap telemetry (may be null)
     * @param telemetryMaxAge     maximum age of station telemetry
     * @param now                 evaluation instant
     * @return true when station should be included in swap pull responses
     */
    public static boolean isEligible(StationSwapState swapState, Duration telemetryMaxAge, Instant now) {
        if (swapState == null) {
            return false;
        }
        if (swapState.getChargedPacksReady() <= 0 || !swapState.isSwapBayFree()) {
            return false;
        }
        Instant cutoff = now.minus(telemetryMaxAge);
        return !swapState.getTelemetryUpdatedAt().isBefore(cutoff);
    }

    /**
     * @param swapState           station swap telemetry (may be null)
     * @param telemetryMaxAge     maximum age of station telemetry
     * @param now                 evaluation instant
     * @return true when telemetry exists but is too old
     */
    public static boolean isStale(StationSwapState swapState, Duration telemetryMaxAge, Instant now) {
        if (swapState == null) {
            return false;
        }
        Instant cutoff = now.minus(telemetryMaxAge);
        return swapState.getTelemetryUpdatedAt().isBefore(cutoff);
    }
}
