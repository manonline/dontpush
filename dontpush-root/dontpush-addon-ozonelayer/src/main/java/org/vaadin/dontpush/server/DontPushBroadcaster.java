
package org.vaadin.dontpush.server;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.BroadcasterCache;
import org.atmosphere.cpr.BroadcasterConfig;
import org.atmosphere.util.SimpleBroadcaster;

public class DontPushBroadcaster extends SimpleBroadcaster {

    public DontPushBroadcaster(String id, AtmosphereConfig config) {
        super(id, config);
    }

    @Override
    protected BroadcasterConfig createBroadcasterConfig(AtmosphereConfig config){
        BroadcasterConfig broadcasterConfig = (BroadcasterConfig)config.properties().get(BroadcasterConfig.class.getName());
        if (broadcasterConfig == null) {
            broadcasterConfig = new BroadcasterConfig(config.framework().broadcasterFilters(), config, false);
            config.properties().put(BroadcasterConfig.class.getName(), broadcasterConfig);
        }
        BroadcasterCache cache = broadcasterConfig.getBroadcasterCache();
        if (cache == null || !DontPushBroadcasterCache.class.isAssignableFrom(broadcasterConfig.getClass()))
            broadcasterConfig.setBroadcasterCache(new DontPushBroadcasterCache());
        return broadcasterConfig;
    }

}
