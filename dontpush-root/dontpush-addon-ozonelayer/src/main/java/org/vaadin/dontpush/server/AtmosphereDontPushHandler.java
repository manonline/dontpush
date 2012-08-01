/*
   Copyright [2011] [Vaadin Ltd]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.vaadin.dontpush.server;

import com.vaadin.ui.Window;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;
import org.atmosphere.gwt.server.GwtAtmosphereResource;

public class AtmosphereDontPushHandler extends AtmosphereGwtHandler {

    private static final ThreadLocal<BroadcasterVaadinSocket> CURRENT_SOCKET = new ThreadLocal<BroadcasterVaadinSocket>();
    private Class<BroadcasterVaadinSocket> socketClass;
    private final Map<GwtAtmosphereResource, BroadcasterVaadinSocket> resourceSocketMap =
      new WeakHashMap<GwtAtmosphereResource, BroadcasterVaadinSocket>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        setSocketClass(servletConfig.getInitParameter("socketClass"));
    }

    @SuppressWarnings("unchecked")
    public void setSocketClass(String socketClassName) {
        if (socketClassName != null) {
            try {
                this.socketClass = (Class<BroadcasterVaadinSocket>) Class
                        .forName(socketClassName);
            } catch (Exception e) {
                this.logger.error("Error loading socket class `"
                        + socketClassName + "'", e);
                this.socketClass = null;
            }
        }
    }

    private void cleanup(GwtAtmosphereResource resource) {
        this.resourceSocketMap.remove(resource);
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Have " + this.resourceSocketMap.size() + " sockets after removal of resource `"
              + resource.getConnectionID() + "'");
        }
        // other cleanup???
    }

    @Override
    public void broadcast(Object message, GwtAtmosphereResource resource) {
        BroadcasterVaadinSocket socket = CURRENT_SOCKET.get();
        if (socket != null) {
            String data = message.toString();
            socket.handlePayload(data);
        } else {
            this.logger.info("Could not handle msg, cm not found. (non-functional) close request??");
        }
    }

    @Override
    public void broadcast(List<?> messages, GwtAtmosphereResource resource) {
        if (messages == null) {
            return;
        }
        for (Object o : messages) {
            this.broadcast(o, resource);
        }
    }

    @Override
    public int doComet(GwtAtmosphereResource resource) throws ServletException,
            IOException {
        establishConnection(resource);
        return NO_TIMEOUT;
    }

    private void establishConnection(final GwtAtmosphereResource resource) {
        final String path = resource.getRequest().getPathInfo();
        String[] split = path.split("/");
        String cmId = split[1];
        final SocketCommunicationManager cm = getCommunicationManager(cmId);
        if (cm == null) {
            this.logger
                    .debug("Couldn't establish connection, no CM found for this session "
                            + cmId);
            // TODO can happen e.g. server restart, should cause reload, now
            // dies silently?
            return;
        }
        String windowName = split[2];
        Window window;
        if ("null".equals(windowName)) {
            window = cm.getApplication().getMainWindow();
            windowName = window.getName();
        } else {
            window = cm.getApplication().getWindow(windowName);
        }

        /*
         * TODO check and handle possible timing issues when renewing the
         * "Socket" with long polling. Currently changes can get lost if server
         * side change exactly when socket is renewed? Should use some
         * atmosphere cache?
         */

        final String key = "dontpush-" + cmId + "-" + windowName;
        final BroadcasterFactory factory = DefaultBroadcasterFactory.getDefault();
        Broadcaster bc = factory.lookup(DefaultBroadcaster.class, key, true);
        if (bc.isDestroyed()) { // handle case of window detach then re-attach
            factory.remove(bc, key);
            bc = factory.lookup(DefaultBroadcaster.class, key, true);
        }
        resource.getAtmosphereResource().setBroadcaster(bc);
        resource.getAtmosphereResource().addEventListener(new AtmosphereResourceEventListenerAdapter() {

            public void onSuspend(AtmosphereResourceEvent event) {
                logger.debug("connection suspended");
                logger.debug("Have " + resourceSocketMap.size() + " sockets after suspend");
            }

            public void onResume(AtmosphereResourceEvent event) {
                logger.debug("connection resumed");
                // cannot call cleanup here as this event is fired before we process data for window close events; thus, if we remove it
                // the UIDL for the close event will never get processed ;0(
                // TODO: maybe remove after some fixed delay

                logger.debug("Have " + resourceSocketMap.size() + " sockets after resume");
            }

            public void onDisconnect(AtmosphereResourceEvent event) {
                logger.debug("connection disconnected; cleaning up");
                cleanup(resource);
            }

            public void onThrowable(AtmosphereResourceEvent event) {
                logger.debug("connection thre exception; cleaning up", event.throwable());
                cleanup(resource);
            }
        });

        VaadinWebSocket socket = cm.getSocketForWindow(window);
        if (socket == null) {
            socket = createSocket(bc, cm, window);
            cm.setSocket(socket, window);
        }
        this.resourceSocketMap.put(resource, (BroadcasterVaadinSocket)socket);
        this.logger.debug("doComet: Connected to CM " + cmId + "; window " + windowName);
    }

    protected BroadcasterVaadinSocket createSocket(Broadcaster resource,
            SocketCommunicationManager cm, Window window) {
        if (this.socketClass != null) {
            try {
                return this.socketClass.getConstructor(
                        Broadcaster.class,
                        SocketCommunicationManager.class, Window.class)
                        .newInstance(resource, cm, window);
            } catch (Exception e) {
                this.logger.error("Error creating socket", e);
            }
        }
        return new BroadcasterVaadinSocket(resource, cm, window);
    }

    @Override
    public void doPost(HttpServletRequest postRequest, HttpServletResponse postResponse,
      List<?> messages, GwtAtmosphereResource cometResource) {
        this.logger.error("TODO Never happens in our case?");
    }

    /**
     * This map is used instead of session as the session is not available in
     * all web socket implementations.
     */
    private static Map<String, SocketCommunicationManager> sessToMgr = Collections
            .synchronizedMap(new HashMap<String, SocketCommunicationManager>());

    public static void setCommunicationManager(String cmId,
            SocketCommunicationManager mgr) {
        sessToMgr.put(cmId, mgr);
    }

    public static SocketCommunicationManager getCommunicationManager(String cmId) {
        return sessToMgr.get(cmId);
    }

    public static void forgetCommunicationManager(String id) {
        sessToMgr.remove(id);
    }

    @Override
    protected void reapResources() {
        super.reapResources();
        for (Iterator<GwtAtmosphereResource> iter = this.resourceSocketMap.keySet().iterator(); iter.hasNext(); ) {
            GwtAtmosphereResource resource = iter.next();
            if (!resource.isAlive()) {
                iter.remove();
            }
        }
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Have " + this.resourceSocketMap.size() + " resources after reaping the dead.");
        }
    }
}
