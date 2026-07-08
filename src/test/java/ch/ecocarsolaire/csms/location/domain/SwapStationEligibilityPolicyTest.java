package ch.ecocarsolaire.csms.location.domain;

import ch.ecocarsolaire.csms.station.domain.StationSwapState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwapStationEligibilityPolicyTest {

    private static final Duration MAX_AGE = Duration.ofMinutes(5);
    private static final Instant NOW = Instant.parse("2026-07-01T18:00:00Z");

    @Test
    void eligible_whenChargedPacksAndFreeBayAndFreshTelemetry() {
        StationSwapState state = new StationSwapState("Station-A", 3, true, 45.0, NOW.minusSeconds(60));

        assertTrue(SwapStationEligibilityPolicy.isEligible(state, MAX_AGE, NOW));
    }

    @Test
    void notEligible_whenNoChargedPacks() {
        StationSwapState state = new StationSwapState("Station-B", 0, true, 10.0, NOW);

        assertFalse(SwapStationEligibilityPolicy.isEligible(state, MAX_AGE, NOW));
    }

    @Test
    void notEligible_whenBayOccupied() {
        StationSwapState state = new StationSwapState("Station-C", 2, false, 10.0, NOW);

        assertFalse(SwapStationEligibilityPolicy.isEligible(state, MAX_AGE, NOW));
    }

    @Test
    void notEligible_whenTelemetryStale() {
        StationSwapState state = new StationSwapState("Station-A", 2, true, 10.0, NOW.minus(Duration.ofMinutes(6)));

        assertFalse(SwapStationEligibilityPolicy.isEligible(state, MAX_AGE, NOW));
        assertTrue(SwapStationEligibilityPolicy.isStale(state, MAX_AGE, NOW));
    }

    @Test
    void notEligible_whenSwapStateMissing() {
        assertFalse(SwapStationEligibilityPolicy.isEligible(null, MAX_AGE, NOW));
    }
}
