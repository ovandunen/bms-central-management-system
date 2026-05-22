package com.solarcsms.location.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.UUID;

/**
 * Aggregate root storing geospatial position of a charging station.
 */
@Entity
@Table(name = "station_location")
public class StationLocation {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "station_id", nullable = false, unique = true, length = 64)
    private String stationId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "solar_capacity_kw")
    private double solarCapacityKw;

    @Column(name = "location", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

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
        this.id = UUID.randomUUID();
        this.stationId = stationId;
        this.displayName = displayName;
        this.solarCapacityKw = solarCapacityKw;
        this.location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
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

    public double getSolarCapacityKw() {
        return solarCapacityKw;
    }

    public Point getLocation() {
        return location;
    }

    /**
     * Returns latitude in degrees.
     *
     * @return latitude
     */
    public double getLatitude() {
        return location.getY();
    }

    /**
     * Returns longitude in degrees.
     *
     * @return longitude
     */
    public double getLongitude() {
        return location.getX();
    }
}
