
package org.vaadin.dontpush.server;

import org.atmosphere.cache.AbstractBroadcasterCache;
import org.atmosphere.cpr.AtmosphereResource;

/**
 * Single cache per broadcaster; just hold reference to last message seen
 */
public class DontPushBroadcasterCache extends AbstractBroadcasterCache {

    private CachedMessage cachedMessage;

    public synchronized void cache(String id, AtmosphereResource r, CachedMessage cm) {
        this.cachedMessage = cm;
    }

    public synchronized CachedMessage retrieveLastMessage(String id, AtmosphereResource r) {
        return this.cachedMessage;
    }
}
