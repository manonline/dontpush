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
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;
import org.atmosphere.gwt.server.GwtAtmosphereResource;

public class AtmosphereDontPushHandler extends AtmosphereGwtHandler {

    private Class<BroadcasterVaadinSocket> socketClass;

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

    @Override
    public void broadcast(Serializable message, GwtAtmosphereResource resource) {
        BroadcasterVaadinSocket socket =
          (BroadcasterVaadinSocket)resource.getSession().getAttribute("" + resource.getConnectionID());
        if (socket != null) {
            String data = message.toString();
            socket.handlePayload(data);
        } else {
            logger.info("Could not handle msg, cm not found. (non-functional) close request??");
        }
    }

    @Override
    public int doComet(GwtAtmosphereResource resource) throws ServletException,
            IOException {
        establishConnection(resource);
        return NO_TIMEOUT;
    }

    private void establishConnection(GwtAtmosphereResource resource) {
        final String path = resource.getRequest().getPathInfo();
        String[] split = path.split("/");
        String cmId = split[1];
        final SocketCommunicationManager cm = getCommunicationManager(cmId);
        if (cm == null) {
            this.logger
                    .debug("Couldn't establish connection, no CM found for this session "
                            + cmId);
            // TODO can happen e.g. server restart, should cause relaod, now
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
        final Broadcaster bc = DefaultBroadcasterFactory.getDefault().lookup(
                DefaultBroadcaster.class, key, true);
        resource.getAtmosphereResource().setBroadcaster(bc);

        VaadinWebSocket socket = cm.getSocketForWindow(window);
        if (socket == null) {
            socket = createSocket(bc, cm, window);
            cm.setSocket(socket, window);
        }
        resource.getSession().setAttribute("" + resource.getConnectionID(), socket);
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
    public void doPost(List<Serializable> messages, GwtAtmosphereResource r) {
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

    public static void forgetCommunicationMananer(String id) {
        sessToMgr.remove(id);
    }
}
