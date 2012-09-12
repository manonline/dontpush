
package org.vaadin.dontpush.server;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.atmosphere.cache.AbstractBroadcasterCache;
import org.atmosphere.config.service.BroadcasterCacheService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcasterCache;

@BroadcasterCacheService
public class DontPushBroadcasterCache implements BroadcasterCache {

    private final BroadcasterCache delegate = new DelegateBroadcasterCache();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public void start() {
        if (!started.getAndSet(true))
            this.delegate.start();
    }

    public void stop() {
        this.delegate.stop();
    }

    public void addToCache(String id, AtmosphereResource r, Object e) {
        this.delegate.addToCache(id, r, e);
    }

    public List<Object> retrieveFromCache(String id, AtmosphereResource r) {
        return this.delegate.retrieveFromCache(id, r);
    }

    private final class DelegateBroadcasterCache extends AbstractBroadcasterCache {

        private final Map<String, CachedMessage> cachedMessageMap = new WeakHashMap<String, CachedMessage>();

        public void cache(String id, AtmosphereResource r, CachedMessage cm) {
            this.cachedMessageMap.put(id, cm);
        }

        public CachedMessage retrieveLastMessage(String id, AtmosphereResource r) {
            return this.cachedMessageMap.get(id);
        }
    }
}
