package ch.ecocarsolaire.csms.location.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.ecocarsolaire.csms.location.domain.LocationRepository;
import ch.ecocarsolaire.csms.location.domain.StationLocation;
import ch.ecocarsolaire.csms.location.domain.SwapStationEligibilityPolicy;
import ch.ecocarsolaire.csms.location.infrastructure.DynamicMqttPublisher;
import ch.ecocarsolaire.csms.station.domain.StationRepository;
import ch.ecocarsolaire.csms.station.domain.StationStatus;
import ch.ecocarsolaire.csms.station.domain.StationSwapState;
import ch.ecocarsolaire.csms.station.domain.StationSwapStateRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Answers nearby-station queries by publishing results over MQTT.
 */
@ApplicationScoped
public class LocationQueryService {

    private static final Logger LOG = Logger.getLogger(LocationQueryService.class);
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final String EMPTY_MESSAGE =
            "No swap stations available within range with charged packs";

    private final LocationRepository locationRepository;
    private final StationRepository stationRepository;
    private final StationSwapStateRepository swapStateRepository;
    private final DynamicMqttPublisher mqttPublisher;
    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "app.location.default-radius-m", defaultValue = "50000")
    double defaultRadiusMeters;

    @ConfigProperty(name = "app.location.swap-telemetry-max-age-minutes", defaultValue = "5")
    long swapTelemetryMaxAgeMinutes;

    public LocationQueryService(LocationRepository locationRepository,
                                StationRepository stationRepository,
                                StationSwapStateRepository swapStateRepository,
                                DynamicMqttPublisher mqttPublisher,
                                ObjectMapper objectMapper) {
        this.locationRepository = locationRepository;
        this.stationRepository = stationRepository;
        this.swapStateRepository = swapStateRepository;
        this.mqttPublisher = mqttPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles a {@link NearbyStationQuery} CDI event from the driver context.
     *
     * @param query nearby station search request
     */
    public void onNearbyStationQuery(@Observes NearbyStationQuery query) {
        try {
            double radius = query.radiusMeters() > 0 ? query.radiusMeters() : defaultRadiusMeters;
            final List<StationLocation> nearby = locationRepository.findNearby(
                    query.coordinate().latitude(),
                    query.coordinate().longitude(),
                    radius);

            final Duration maxAge = Duration.ofMinutes(swapTelemetryMaxAgeMinutes);
            final Instant now = Instant.now();
            final List<StationLocationDto> payload = new ArrayList<>();

            for (StationLocation loc : nearby) {
                var stationOpt = stationRepository.findByStationId(loc.getStationId());
                if (stationOpt.isEmpty()
                        || stationOpt.get().getStatus() != StationStatus.AVAILABLE) {
                    continue;
                }

                final StationSwapState swapState = swapStateRepository.findByStationId(loc.getStationId()).orElse(null);
                if (SwapStationEligibilityPolicy.isStale(swapState, maxAge, now)) {
                    LOG.warnf("Excluding station %s — swap telemetry stale", loc.getStationId());
                    continue;
                }
                if (!SwapStationEligibilityPolicy.isEligible(swapState, maxAge, now)) {
                    continue;
                }

                final double distanceKm = haversineMeters(
                        query.coordinate().latitude(),
                        query.coordinate().longitude(),
                        loc.getLatitude(),
                        loc.getLongitude()) / 1000.0;
                payload.add(StationLocationDto.from(
                        loc, stationOpt.get().getStatus(), swapState, distanceKm));
            }

            final StationAvailabilityResponse response = payload.isEmpty()
                    ? StationAvailabilityResponse.empty(query.correlationId(), EMPTY_MESSAGE)
                    : StationAvailabilityResponse.withStations(query.correlationId(), payload);

            final String topic = "stations/available/" + query.driverId();
            final byte[] body = objectMapper.writeValueAsBytes(response);
            mqttPublisher.publish(topic, body);
            LOG.infof("Published %d swap stations to %s (correlationId=%s)",
                    payload.size(), topic, query.correlationId());
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize station locations for driver %s", query.driverId());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process nearby station query for driver %s", query.driverId());
        }
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
