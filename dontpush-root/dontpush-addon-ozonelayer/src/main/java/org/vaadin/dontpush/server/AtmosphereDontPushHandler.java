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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;
import org.atmosphere.gwt.server.GwtAtmosphereResource;
import org.atmosphere.gwt.server.SerializationException;
import org.atmosphere.gwt.shared.Constants;
import org.atmosphere.gwt.shared.SerialMode;

public class AtmosphereDontPushHandler extends AtmosphereGwtHandler {

    private Class<BroadcasterVaadinSocket> socketClass;
    private final Map<GwtAtmosphereResource, BroadcasterVaadinSocket> resourceSocketMap = new WeakHashMap<GwtAtmosphereResource, BroadcasterVaadinSocket>();
    private final Map<GwtAtmosphereResource, SerialMode> resourceSerialModeMap = new WeakHashMap<GwtAtmosphereResource, SerialMode>();
    private final Set<GwtAtmosphereResource> resourcesProcessingData = new HashSet<GwtAtmosphereResource>();

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void cometTerminated(GwtAtmosphereResource cometResponse, boolean serverInitiated) {
        Set<GwtAtmosphereResource> resources = new HashSet<GwtAtmosphereResource>(this.resourceSocketMap.keySet());
        for (GwtAtmosphereResource resource : resources) {
            if (resource.getConnectionID() == cometResponse.getConnectionID()) {
                this.resourceSocketMap.remove(resource);
            }
        }
        resources = new HashSet<GwtAtmosphereResource>(this.resourceSerialModeMap.keySet());
        for (GwtAtmosphereResource resource : resources) {
            if (resource.getConnectionID() == cometResponse.getConnectionID()) {
                this.resourceSerialModeMap.remove(resource);
            }
        }

        this.logger.debug("Have " + this.resourceSocketMap.size()
          + " sockets after removal of resource `"
          + cometResponse.getConnectionID() + "'");
    }

    @Override
    public void broadcast(Object message, GwtAtmosphereResource resource) {
        BroadcasterVaadinSocket socket = resourceSocketMap.get(resource);
        if (socket != null) {
            String data = message.toString();
            socket.handlePayload(data);
        } else {
            this.logger
                    .info("Could not handle msg, cm not found. (non-functional) close request??");
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
        SocketCommunicationManager cm = null;
        String path = resource.getRequest().getPathInfo();
        String windowName = "null";
        if (path == null) {
            // Glassfish haxies, pathinfo dont work, session dont work
            String queryString = resource.getRequest().getQueryString();
            if (queryString != null && queryString.contains("w=")) {
                windowName = queryString
                        .substring(queryString.indexOf("w=") + 2);
                if (windowName.contains("&")) {
                    windowName = windowName.substring(0,
                            windowName.indexOf("&"));
                }
                String cmId = queryString.substring(queryString
                        .indexOf("cmid=") + 5);
                if (cmId.contains("&")) {
                    cmId = cmId.substring(0, cmId.indexOf("&"));
                }
                cm = SocketCommunicationManager.get(cmId);
            }
        } else {
            String[] split = path.split("/");
            windowName = split[1];
        }

        if (cm == null) {
            HttpSession session = resource.getSession();
            cm = (SocketCommunicationManager) session
                    .getAttribute(SocketCommunicationManager.class.getName());
        }
        if (cm == null) {
            this.logger
                    .debug("Couldn't establish connection, no CM found for this session");
            // TODO can happen e.g. server restart, should cause reload, now
            // dies silently?
            return;
        }
        cm.setExecutor(this);
        String cmId = cm.getId();
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
        System.err.println(key);

        final BroadcasterFactory factory = DefaultBroadcasterFactory
                .getDefault();
        Broadcaster bc = factory.lookup(DontPushBroadcaster.class, key, true);
        if (bc.isDestroyed()) { // handle case of window detach then re-attach
            factory.remove(bc, key);
            bc = factory.lookup(DontPushBroadcaster.class, key, true);
        }

        resource.getAtmosphereResource().setBroadcaster(bc);
        resource.getAtmosphereResource().addEventListener(
                new AtmosphereResourceEventListenerAdapter() {

                    public void onSuspend(AtmosphereResourceEvent event) {
                        logger.debug("connection suspended");
                        logger.debug("Have " + resourceSocketMap.size()
                                + " sockets after suspend");
                    }

                    public void onResume(AtmosphereResourceEvent event) {
                        logger.debug("connection resumed");
                        // cannot call cleanup here as this event is fired
                        // before we process data for window close events; thus,
                        // if we remove it
                        // the UIDL for the close event will never get processed
                        // ;0(
                        // TODO: maybe remove after some fixed delay

                        logger.debug("Have " + resourceSocketMap.size()
                                + " sockets after resume");
                    }

                    public void onDisconnect(AtmosphereResourceEvent event) {
                        logger.debug("connection disconnected; cleaning up");
                    }

                    public void onThrowable(AtmosphereResourceEvent event) {
                        logger.debug("connection thre exception; cleaning up",
                                event.throwable());
                    }
                });

        VaadinWebSocket socket = cm.getSocketForWindow(window);
        if (socket == null) {
            socket = createSocket(bc, cm, window);
            cm.setSocket(socket, window);
        }

        this.resourceSocketMap.put(resource, (BroadcasterVaadinSocket) socket);
        this.logger.debug("doComet: Connected to CM " + cmId + "; window "
                + windowName);
    }

    protected BroadcasterVaadinSocket createSocket(Broadcaster resource,
            SocketCommunicationManager cm, Window window) {
        if (this.socketClass != null) {
            try {
                return this.socketClass.getConstructor(Broadcaster.class,
                        SocketCommunicationManager.class, Window.class)
                        .newInstance(resource, cm, window);
            } catch (Exception e) {
                this.logger.error("Error creating socket", e);
            }
        }
        return new BroadcasterVaadinSocket(resource, cm, window);
    }

    @Override
    public void doPost(HttpServletRequest postRequest,
            HttpServletResponse postResponse, List<?> messages,
            GwtAtmosphereResource cometResource) {
        this.logger.error("TODO Never happens in our case?");
    }

    @Override
    protected void reapResources() {
        super.reapResources();
        for (GwtAtmosphereResource resource : this.resourceSocketMap.keySet()) {
            synchronized (this.resourcesProcessingData) {
                if (!resource.isAlive() && !this.resourcesProcessingData.contains(resource)) {
                    this.resourceSocketMap.remove(resource);
                }
            }
        }
        for (GwtAtmosphereResource resource : this.resourceSerialModeMap.keySet()) {
            if (!resource.isAlive()) {
                this.resourceSerialModeMap.remove(resource);
            }
        }
        this.logger.debug("Have " + this.resourceSocketMap.size()
          + " resources after reaping the dead.");
    }

    @Override
    protected void doServerMessage(HttpServletRequest request, HttpServletResponse response, int connectionID)
      throws IOException{
        BufferedReader data = request.getReader();
        List<Object> postMessages = new ArrayList<Object>();
        GwtAtmosphereResource resource = lookupResource(connectionID);
        if (resource == null) {
            return;
        }

        final SerialMode serialMode = this.getSerialMode(resource);

        try {
            synchronized (this.resourcesProcessingData) {
                this.resourcesProcessingData.add(resource);
            }
            while (true) {
                String event = data.readLine();
                if (event == null) {
                    break;
                }
                String action = data.readLine();

                if (logger.isTraceEnabled()) {
                    logger.trace("[" + connectionID + "] Server message received: " + event + ";" + action);
                }
                if (event.equals("o") || event.equals("s")) {
                    int length = Integer.parseInt(data.readLine());
                    char[] messageData = new char[length];
                    int totalRead = 0;
                    int read;
                    while ((read = data.read(messageData, totalRead, length - totalRead)) != -1) {
                        totalRead += read;
                        if (totalRead == length) {
                            break;
                        }
                    }
                    if (totalRead != length) {
                        throw new IllegalStateException("Corrupt message received");
                    }
                    Object message = null;
                    if (event.equals("o")) {
                        try {
                            message = deserialize(messageData, serialMode);
                        } catch (SerializationException ex) {
                            logger.error("Failed to deserialize message", ex);
                        }
                    } else {
                        message = String.copyValueOf(messageData);
                    }
                    if (message != null) {
                        if (action.equals("p")) {
                            postMessages.add(message);
                        } else if (action.equals("b")) {
                            broadcast(message, resource);
                        }
                    }
                } else if (event.equals("c")) {
                    if (action.equals("d")) {
                        disconnect(resource);
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("[" + connectionID + "] Failed to read", ex);
        } finally {
            synchronized (this.resourcesProcessingData) {
                this.resourcesProcessingData.remove(resource);
            }
        }

        if (postMessages.size() > 0) {
            post(request, response, postMessages, resource);
        }
    }

    private SerialMode getSerialMode(GwtAtmosphereResource resource) {
        SerialMode serialMode = this.resourceSerialModeMap.get(resource);
        if (resource.isAlive()) {
            String mode = resource.getRequest().getParameter(Constants.CLIENT_SERIALZE_MODE_PARAMETER);
            if (mode != null)
                serialMode = SerialMode.valueOf(mode);
        }
        if (serialMode == null)
            serialMode = this.getDefaultSerialMode();

        this.resourceSerialModeMap.put(resource, serialMode);

        return serialMode;
    }

    /**
     * <p>
     * Specifies the default {@link SerialMode} for this {@link org.atmosphere.cpr.AtmosphereHandler}.  This value is used if no
     * serial mode parameter is sent with the suspended request.
     * @return default {@link SerialMode} if not specified in the suspended request's parameter map
     */
    protected SerialMode getDefaultSerialMode() {
        return SerialMode.PLAIN;
    }
}
