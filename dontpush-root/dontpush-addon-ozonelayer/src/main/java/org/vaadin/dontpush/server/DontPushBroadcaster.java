
package org.vaadin.dontpush.server;

import java.util.Collections;
import java.util.List;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcasterCache;
import org.atmosphere.cpr.BroadcasterConfig;
import org.atmosphere.util.SimpleBroadcaster;

public class DontPushBroadcaster extends SimpleBroadcaster {

    private static final BroadcasterCache DUMMY_CACHE = new BroadcasterCache() {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void addToCache(String id, AtmosphereResource r, Object e) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> retrieveFromCache(String id, AtmosphereResource r) {
            return Collections.EMPTY_LIST;
        }
    };

    public DontPushBroadcaster(String id, AtmosphereConfig config) {
        super(id, config);
    }

    /**
     * <p>
     * Override to ensure we have a dummy {@link BroadcasterCache} set on the {@link BroadcasterConfig} instance.  This is necessary
     * to alleviate user ignorance in setting one of the global {@link BroadcasterCache} implementations shipped with Atmosphere.
     * </p>
     *
     * <p>
     * The reason the built-in implementations do not work is that they broadcast all message since the last seen message with no
     * regard to whether or not the messages in the cache queue are applicable to the given {@link AtmosphereResource}.
     * Additionally, the {@link org.atmosphere.cpr.AtmosphereResourceEvent} object is set with a list of messages in the case there
     * were missed messages and the {@link org.vaadin.dontpush.widgetset.client.SocketApplicationConnection} is not setup to handle
     * JSON arrays; it's more complexity than is necessary.
     * </p>
     *
     * {@inheritDoc}
     *
     * @param config {@link AtmosphereConfig}
     * @return shared {@link BroadcasterConfig} within the {@link AtmosphereConfig}
     */
    @Override
    protected BroadcasterConfig createBroadcasterConfig(AtmosphereConfig config){
        BroadcasterConfig broadcasterConfig = super.createBroadcasterConfig(config);
        BroadcasterCache cache = broadcasterConfig.getBroadcasterCache();
        if (cache != DUMMY_CACHE)
            broadcasterConfig.setBroadcasterCache(DUMMY_CACHE);
        return broadcasterConfig;
    }

}
