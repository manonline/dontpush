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
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

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

    public String getSocketClass() {
        return this.socketClass == null ? null : this.socketClass.getName();
    }

    @SuppressWarnings("unchecked")
    public void setSocketClass(String socketClassName) {
        if (socketClassName != null) {
            try {
                this.socketClass = (Class<BroadcasterVaadinSocket>)Class.forName(socketClassName);
            } catch (Exception e) {
                this.logger.error("Error loading socket class `" + socketClassName + "'", e);
                this.socketClass = null;
            }
        }
    }

    @Override
    public void broadcast(Serializable message, GwtAtmosphereResource resource) {
        BroadcasterVaadinSocket socket = resource.getAttribute(BroadcasterVaadinSocket.class.getName());
        String data = message.toString();
        socket.handlePayload(data);
    }

    @Override
    public int doComet(GwtAtmosphereResource resource) throws ServletException, IOException {

        /*
         * TODO expect problems here. Session, websocket grizzly ~ nogo or
         * athmosphere does some magic i don't know about. Prepare to connect to
         * session by request path
         */
        HttpSession session = resource.getSession(false);
        if (session != null) {
            /*
             * TODO check and handle
             * possible timing issues when renewing the "Socket" with long
             * polling. Currently changes can get lost if server side change exactly when socket is renewed?
             */

            final String path = resource.getRequest().getPathInfo();
            final String windowName = path.substring(path.lastIndexOf("/") + 1);
            final String key = "dontpush-" + session.getId() + "-" + windowName;
            final Broadcaster bc = DefaultBroadcasterFactory.getDefault().lookup(DefaultBroadcaster.class, key, true);
            resource.getAtmosphereResource().setBroadcaster(bc);

            if (session.getAttribute(key) == null) {
                session.setAttribute(key, new BroadcasterCleaner(key));
            }

            final SocketCommunicationManager cm =
              (SocketCommunicationManager)session.getAttribute(SocketCommunicationManager.class.getName());

            if (cm != null) {
                Window window;
                if ("null".equals(windowName)) {
                    window = cm.getApplication().getMainWindow();
                } else {
                    window = cm.getApplication().getWindow(windowName);
                }
                BroadcasterVaadinSocket socket = createSocket(resource, cm, window);
                resource.setAttribute(BroadcasterVaadinSocket.class.getName(), socket);
                cm.setSocket(socket, window);
                this.logger.debug("doComet: Connected to CM" + session.getId());
            }
        }

        return NO_TIMEOUT;
    }

    protected BroadcasterVaadinSocket createSocket(GwtAtmosphereResource resource, SocketCommunicationManager cm, Window window) {
        if (this.socketClass != null) {
            try {
                return this.socketClass.getConstructor(GwtAtmosphereResource.class, SocketCommunicationManager.class, Window.class)
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

    static class BroadcasterCleaner implements HttpSessionBindingListener, Serializable {

        private String key;

        public BroadcasterCleaner(String key) {
            this.key = key;
        }

        public void valueBound(HttpSessionBindingEvent event) {
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            Broadcaster lookup = DefaultBroadcasterFactory.getDefault().lookup(DefaultBroadcaster.class, key, false);
            if (lookup != null) {
                DefaultBroadcasterFactory.getDefault().remove(lookup, this.key);
            }
        }
    }
}
