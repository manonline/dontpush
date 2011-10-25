/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vaadin.dontpush.server;

import com.vaadin.ui.Window;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

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

    @Override
    public void broadcast(Serializable message, GwtAtmosphereResource resource) {
        BroadCasterVaadinSocket socket = resource
                .getAttribute(BroadCasterVaadinSocket.class.getName());
        String data = message.toString();
        socket.handlePayload(data);
    }

    @Override
    public int doComet(GwtAtmosphereResource resource) throws ServletException,
            IOException {

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

            String path = resource.getRequest().getPathInfo();
            String windowName = path.substring(path.lastIndexOf("/"));
            String key = "dontpush-" + session.getId() + "-" + windowName;
            Broadcaster bc = DefaultBroadcasterFactory.getDefault().lookup(
                    DefaultBroadcaster.class,
                    key , true);
            resource.getAtmosphereResource().setBroadcaster(bc);

            if(session.getAttribute(key) == null) {
                session.setAttribute(key, new BroadcasterCleaner(key));
            }

            SocketCommunicationManager cm = (SocketCommunicationManager) session
                    .getAttribute(SocketCommunicationManager.class.getName());
            Window window;
            if ("/null".equals(windowName)) {
                window = cm.getApplication().getMainWindow();
            } else {
                window = cm.getApplication().getWindow(windowName);
            }
            BroadCasterVaadinSocket socket = new BroadCasterVaadinSocket(
                    resource, cm, window);
            resource.setAttribute(BroadCasterVaadinSocket.class.getName(),
                    socket);
            cm.setSocket(socket, window);
            Logger.getLogger(getClass().getName()).fine("doComet: Connected to CM" + session.getId());
        }

        return NO_TIMEOUT;
    }

    @Override
    public void doPost(List<Serializable> messages, GwtAtmosphereResource r) {
        Logger.getLogger(getClass().getName()).severe(
                "TODO Never happens in our case?");
    }

    static class BroadcasterCleaner implements HttpSessionBindingListener {

        private String key;

        public BroadcasterCleaner(String key) {
            this.key = key;
        }

        public void valueBound(HttpSessionBindingEvent event) {
            // TODO Auto-generated method stub

        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            Broadcaster lookup = DefaultBroadcasterFactory.getDefault().lookup(DefaultBroadcaster.class, key, false);
            if(lookup != null) {
                DefaultBroadcasterFactory.getDefault().remove(lookup, key);
            }
        }

    }

}
