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

import com.vaadin.ui.Window;

public class AtmosphereDontPushHandler extends AtmosphereGwtHandler {

    private Class<BroadcasterVaadinSocket> socketClass;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        setSocketClass(servletConfig.getInitParameter("socketClass"));
    }

    public String getSocketClass() {
        return this.socketClass == null ? null : this.socketClass.getName();
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
        BroadcasterVaadinSocket socket = resource
                .getAttribute(BroadcasterVaadinSocket.class.getName());
        if (socket == null) {
            // TODO check if this can really happen and if it helps? Thought it
            // solve one issue with tomcat, but I think it was session mix up
            // error.
            establishConnection(resource);
            socket = resource.getAttribute(BroadcasterVaadinSocket.class
                    .getName());
        }
        if (socket != null) {
            String data = message.toString();
            socket.handlePayload(data);
        } else {
            logger.error("Could not handle msg, cm not found.");
        }
    }

    @Override
    public int doComet(GwtAtmosphereResource resource) throws ServletException,
            IOException {
        establishConnection(resource);
        return NO_TIMEOUT;
    }

    private void establishConnection(GwtAtmosphereResource resource) {
        /*
         * TODO expect problems here. Session, websocket grizzly ~ nogo or
         * athmosphere does some magic i don't know about. Prepare to connect to
         * session by request path
         */
        final String path = resource.getRequest().getPathInfo();
        String[] split = path.split("/");
        String cmId = split[1];
        if ("undefined".equals(cmId)) {
            // httpOnly session e.g. in tomcat7
            // TODO build workaround for this. We don't use session id as it is
            // in some cases faked by atmosphere
            cmId = resource.getRequest().getSession().getId();
        }
        String windowName = split[2];
        /*
         * TODO check and handle possible timing issues when renewing the
         * "Socket" with long polling. Currently changes can get lost if server
         * side change exactly when socket is renewed?
         */

        final String key = "dontpush-" + cmId + "-" + windowName;
        final Broadcaster bc = DefaultBroadcasterFactory.getDefault().lookup(
                DefaultBroadcaster.class, key, true);
        resource.getAtmosphereResource().setBroadcaster(bc);

        final SocketCommunicationManager cm = getCommunicationManager(cmId);

        if (cm != null) {
            Window window;
            if ("null".equals(windowName)) {
                window = cm.getApplication().getMainWindow();
            } else {
                window = cm.getApplication().getWindow(windowName);
            }
            BroadcasterVaadinSocket socket = createSocket(resource, cm, window);
            resource.setAttribute(BroadcasterVaadinSocket.class.getName(),
                    socket);
            cm.setSocket(socket, window);
            this.logger.debug("doComet: Connected to CM" + cmId);
        } else {
            this.logger
                    .debug("Couldn't establish connection, no CM found for this session "
                            + cmId);
        }
    }

    protected BroadcasterVaadinSocket createSocket(
            GwtAtmosphereResource resource, SocketCommunicationManager cm,
            Window window) {
        if (this.socketClass != null) {
            try {
                return this.socketClass.getConstructor(
                        GwtAtmosphereResource.class,
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

    public static SocketCommunicationManager getCommunicationManager(
            String cmId) {
        return sessToMgr.get(cmId);
    }

    public static void forgetCommunicationMananer(String id) {
        sessToMgr.remove(id);
    }
}
