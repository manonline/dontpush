/*
   Copyright [2010] [Vaadin Ltd]

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

package org.vaadin.dontpush.widgetset.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.BrowserInfo;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.WidgetSet;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.atmosphere.gwt.client.AtmosphereClient;
import org.atmosphere.gwt.client.AtmosphereListener;

/**
 * Uses WebSockets instead of XHR's for communicating with server.
 *
 * @author mattitahvonen
 */
public class SocketApplicationConnection extends ApplicationConnection {

    private AtmosphereClient ws;
    private boolean ownRequestPending;

    private AtmosphereListener _cb = new AtmosphereListener() {

        public void onConnected(int heartbeat, int connectionID) {
            VConsole.log("WS Connected");
            if (!applicationRunning) {
                repaintAll();
            }
        }

        public void onBeforeDisconnected() {
            VConsole.log("DEBUG: onbeforediconnected");
            // TODO Auto-generated method stub

        }

        public void onDisconnected() {
            VConsole.log("WS Disconnected");
            // re-open immediately if timeout happened ??
            // TODO I guess this should be configurable for different use cases
            // TODO how to handle windows forgotten open ?
        }

        public void onError(Throwable exception, boolean connected) {
            VConsole.log("DEBUG: onError");
            VConsole.log(exception);

        }

        public void onHeartbeat() {
            VConsole.log("DEBUG: onHeartbeat");
        }

        public void onRefresh() {
            VConsole.log("DEBUG: onRefresh");
        }

        public void onMessage(List<? extends Serializable> messages) {
            for (Serializable serializable : messages) {
                String message = serializable.toString();
                VConsole.log("message");
                if (!ownRequestPending) {
                    startRequest();
                    VConsole.log("Changeset pushed by the server");
                } else {
                    ownRequestPending = false;
                }
                final Date start = new Date();
                message = "{" + message + "}";
                VConsole.log("Received socket message:");
                ValueMap json = evaluateUIDL(message);
                if (applicationRunning) {
                    handleUIDLMessage(start, message, json);
                } else {
                    applicationRunning = true;
                    handleWhenCSSLoaded(message, json);
                    ApplicationConfiguration.startNextApplication();
                }
            }

        }
    };

    @Override
    public void init(WidgetSet widgetSet, ApplicationConfiguration cnf) {
        super.init(widgetSet, cnf);
        // First opening of WS will repaint all -> start the app
        getWebSocket();
    }

    private AtmosphereClient getWebSocket() {
        if (this.ws == null) {
            // if timed out or not started, create websocket to server
            String url = getConfiguration().getApplicationUri() + "UIDL/";
            if(url.startsWith("/")) {
                String hostPageBaseURL = GWT.getHostPageBaseURL();
                String[] split = hostPageBaseURL.split("\\/\\/");
                String host = split[1].substring(0, split[1].indexOf("/"));
                String protoAndHost = split[0] + "//" + host;
                url = protoAndHost + url;
            }

            String cookie = Cookies.getCookie("JSESSIONID");
            url += cookie + "/" + getConfiguration().getInitialWindowName();
            VConsole.log(url);

            boolean webkit = BrowserInfo.get().isWebkit();
            VConsole.log("Creating atmosphere client...");
            /*
             * Ask atmosphere guys to fix this. Automatic degrading from
             * websockets don't work.
             */
            this.ws = new AtmosphereClient(url, null, _cb, webkit);
            VConsole.log("...starting...");
            this.ws.start();
        }
        return this.ws;
    }

    @Override
    public void start() {
        VConsole.log("No real start here");
        /*
         * NOP, init when web sockect connected
         */
    }

    @Override
    protected void makeUidlRequest(String requestData, String extraParams, boolean forceSync) {
        VConsole.log("new Socket message: " + requestData);
        if (forceSync) {
            /*
             * TODO figure out if socket can be used on unload.
             */
            super.makeUidlRequest(requestData, extraParams, forceSync);
        } else {
            startRequest();
            this.ownRequestPending = true;
            getWebSocket().broadcast(extraParams + "#" + requestData);
        }
    }

    private static native ValueMap evaluateUIDL(String jsonText)
    /*-{
        try {
            return JSON.parse(jsonText);
        } catch(ignored) {
            return eval('(' + jsonText + ')');
        }
    }-*/;

}
