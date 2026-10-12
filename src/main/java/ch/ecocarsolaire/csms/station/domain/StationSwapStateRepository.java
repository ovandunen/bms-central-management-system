package ch.ecocarsolaire.csms.station.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Persistence gateway for {@link StationSwapState}.
 */
@ApplicationScoped
public class StationSwapStateRepository implements PanacheRepositoryBase<StationSwapState, String> {

    public Optional<StationSwapState> findByStationId(String stationId) {
        return findByIdOptional(stationId);
    }
}
