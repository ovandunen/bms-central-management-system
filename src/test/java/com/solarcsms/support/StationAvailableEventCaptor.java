package com.solarcsms.support;

import com.solarcsms.station.application.StationAvailableEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test observer capturing {@link StationAvailableEvent} instances.
 */
@ApplicationScoped
public class StationAvailableEventCaptor {

    private final AtomicReference<StationAvailableEvent> lastEvent = new AtomicReference<>();

    /**
     * @param event observed availability event
     */
    public void onAvailable(@Observes StationAvailableEvent event) {
        lastEvent.set(event);
    }

    /**
     * @return last captured event, if any
     */
    public Optional<StationAvailableEvent> lastEvent() {
        return Optional.ofNullable(lastEvent.get());
    }

    /**
     * Clears captured events between tests.
     */
    public void reset() {
        lastEvent.set(null);
    }
}
