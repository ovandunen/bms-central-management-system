package com.solarcsms.support;

import com.solarcsms.location.application.NearbyStationQuery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test observer capturing {@link NearbyStationQuery} events.
 */
@ApplicationScoped
public class NearbyStationQueryCaptor {

    private final AtomicReference<NearbyStationQuery> lastQuery = new AtomicReference<>();

    /**
     * @param query observed nearby station query
     */
    public void onQuery(@Observes NearbyStationQuery query) {
        lastQuery.set(query);
    }

    /**
     * @return last captured query, if any
     */
    public Optional<NearbyStationQuery> lastQuery() {
        return Optional.ofNullable(lastQuery.get());
    }

    /**
     * Clears captured events between tests.
     */
    public void reset() {
        lastQuery.set(null);
    }
}
