package com.solarcsms.driver.domain;

import java.util.UUID;

/**
 * Domain representation of a driver using the mobile app.
 */
public class Driver {

    private final UUID id;
    private final String externalId;

    /**
     * Creates a driver identity.
     *
     * @param externalId MQTT topic / app identifier
     */
    public Driver(String externalId) {
        this.id = UUID.randomUUID();
        this.externalId = externalId;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }
}
