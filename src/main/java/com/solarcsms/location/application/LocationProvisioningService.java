package com.solarcsms.location.application;

import com.solarcsms.location.domain.LocationRepository;
import com.solarcsms.location.domain.StationLocation;
import com.solarcsms.station.application.StationAvailableEvent;
import com.solarcsms.station.application.StationRegisteredEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Ensures {@link StationLocation} rows exist for known charge points.
 * OCPP BootNotification does not include GPS — default coordinates are applied here.
 */
@ApplicationScoped
public class LocationProvisioningService {

    private static final Logger LOG = Logger.getLogger(LocationProvisioningService.class);

    private final LocationRepository locationRepository;

    @ConfigProperty(name = "app.location.demo-default-latitude", defaultValue = "52.52")
    double defaultLatitude;

    @ConfigProperty(name = "app.location.demo-default-longitude", defaultValue = "13.405")
    double defaultLongitude;

    @ConfigProperty(name = "app.location.demo-default-solar-kw", defaultValue = "120")
    double defaultSolarKw;

    public LocationProvisioningService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * After boot, ensure a placeholder location exists so nearby queries can resolve the station
     * once it becomes {@link com.solarcsms.station.domain.StationStatus#AVAILABLE}.
     */
    @Transactional
    public void onStationRegistered(@Observes StationRegisteredEvent event) {
        ensureLocation(event.stationId(), event.stationId() + " Solar Hub");
    }

    /**
     * When a connector reports Available, guarantee geospatial registration for PostGIS queries.
     */
    @Transactional
    public void onStationAvailable(@Observes StationAvailableEvent event) {
        ensureLocation(event.stationId(), event.stationId());
    }

    private void ensureLocation(String stationId, String displayName) {
        if (locationRepository.findByStationId(stationId).isPresent()) {
            return;
        }
        StationLocation location = new StationLocation(
                stationId,
                defaultLatitude,
                defaultLongitude,
                displayName,
                defaultSolarKw);
        locationRepository.persist(location);
        LOG.infof("Provisioned default location for %s at %.4f, %.4f",
                stationId, defaultLatitude, defaultLongitude);
    }
}
