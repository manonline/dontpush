
package org.vaadin.dontpush.server;

import org.atmosphere.cache.AbstractBroadcasterCache;
import org.atmosphere.config.service.BroadcasterCacheService;
import org.atmosphere.cpr.AtmosphereResource;

@BroadcasterCacheService
public class DontPushBroadcasterCache extends AbstractBroadcasterCache {


    private CachedMessage cachedMessage;

    /**
     * {@inheritDoc}
     */
    public synchronized void cache(String id, AtmosphereResource r, CachedMessage cm) {
        if (r != null) {
            this.cachedMessage = cm;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized CachedMessage retrieveLastMessage(String id, AtmosphereResource r) {
        return this.cachedMessage;
    }
}
