package com.solarcsms.notification;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.Radio;

/**
 * No-op {@link Radio} used only to construct {@link eu.chargetime.ocpp.JSONCommunicator} for pack/unpack.
 */
final class NoOpRadio implements Radio {

    @Override
    public void disconnect() {
        // no-op
    }

    @Override
    public void send(Object o) throws NotConnectedException {
        // no-op
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
