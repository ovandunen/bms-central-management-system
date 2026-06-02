package com.solarcsms.location.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarcsms.location.domain.LocationRepository;
import com.solarcsms.location.domain.StationLocation;
import com.solarcsms.location.infrastructure.DynamicMqttPublisher;
import com.solarcsms.station.domain.ChargingStation;
import com.solarcsms.station.domain.StationRepository;
import com.solarcsms.station.domain.StationStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Answers nearby-station queries by publishing results over MQTT.
 */
@ApplicationScoped
public class LocationQueryService {

    private static final Logger LOG = Logger.getLogger(LocationQueryService.class);

    private final LocationRepository locationRepository;
    private final StationRepository stationRepository;
    private final DynamicMqttPublisher mqttPublisher;
    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "app.location.default-radius-m", defaultValue = "50000")
    double defaultRadiusMeters;

    public LocationQueryService(LocationRepository locationRepository,
                                StationRepository stationRepository,
                                DynamicMqttPublisher mqttPublisher,
                                ObjectMapper objectMapper) {
        this.locationRepository = locationRepository;
        this.stationRepository = stationRepository;
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
            List<StationLocation> nearby = locationRepository.findNearby(
                    query.coordinate().latitude(),
                    query.coordinate().longitude(),
                    radius);

            List<StationLocationDto> payload = nearby.stream()
                    .map(loc -> stationRepository.findByStationId(loc.getStationId())
                            .map(cs -> StationLocationDto.from(loc, cs.getStatus()))
                            .orElse(null))
                    .filter(dto -> dto != null && StationStatus.AVAILABLE.name().equals(dto.status()))
                    .toList();

            String topic = "stations/available/" + query.driverId();
            byte[] body = objectMapper.writeValueAsBytes(payload);
            mqttPublisher.publish(topic, body);
            LOG.infof("Published %d stations to %s", payload.size(), topic);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize station locations for driver %s", query.driverId());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process nearby station query for driver %s", query.driverId());
        }
        // TODO: apply driver-specific filters (connector type, pricing, solar preference)
    }
}
