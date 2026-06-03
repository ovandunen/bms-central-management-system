package com.solarcsms.location.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Aggregate root storing geospatial position of a charging station.
 */
@Entity
@Table(name = "station_location")
public class StationLocation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "station_id", nullable = false, unique = true, length = 64)
    private String stationId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "solar_capacity_kw")
    private double solarCapacityKw;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    protected StationLocation() {
        // JPA
    }

    /**
     * Creates a station location aggregate.
     *
     * @param stationId        linked OCPP charge point id
     * @param latitude         WGS-84 latitude
     * @param longitude        WGS-84 longitude
     * @param displayName      human-readable label
     * @param solarCapacityKw  installed solar capacity in kW
     */
    public StationLocation(String stationId, double latitude, double longitude,
                           String displayName, double solarCapacityKw) {
        this(stationId, latitude, longitude, displayName, null, null, solarCapacityKw);
    }

    /**
     * Creates a station location with optional postal address fields.
     */
    public StationLocation(String stationId, double latitude, double longitude,
                           String displayName, String streetAddress, String city,
                           double solarCapacityKw) {
        this.id = UUID.randomUUID();
        this.stationId = stationId;
        this.displayName = displayName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.solarCapacityKw = solarCapacityKw;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getId() {
        return id;
    }

    public String getStationId() {
        return stationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }

    public double getSolarCapacityKw() {
        return solarCapacityKw;
    }

    /**
     * Returns latitude in degrees.
     *
     * @return latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns longitude in degrees.
     *
     * @return longitude
     */
    public double getLongitude() {
        return longitude;
    }
}
