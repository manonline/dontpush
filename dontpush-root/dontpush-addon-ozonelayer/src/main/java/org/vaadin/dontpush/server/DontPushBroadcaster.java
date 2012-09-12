
package org.vaadin.dontpush.server;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.util.SimpleBroadcaster;

public class DontPushBroadcaster extends SimpleBroadcaster {

    public DontPushBroadcaster(String id, AtmosphereConfig config) {
        super(id, config);
    }

    @Override
    protected void start() {
        if (!started.getAndSet(true)) {
            setID(name);
            broadcasterCache = new DontPushBroadcasterCache();
            broadcasterCache.start();
        }
    }
}
