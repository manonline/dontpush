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

package org.vaadin.dontpush.widgetset.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.WidgetSet;

import java.util.Date;
import java.util.List;

import org.atmosphere.gwt.client.AtmosphereClient;
import org.atmosphere.gwt.client.AtmosphereGWTSerializer;
import org.atmosphere.gwt.client.AtmosphereListener;
import org.atmosphere.gwt.shared.SerialMode;

/**
 * Uses WebSockets instead of XHR's for communicating with server.
 *
 * @author mattitahvonen
 */
public class SocketApplicationConnection extends ApplicationConnection {

    public static final String MSG_TERMINATION_STRING = "OZONE\"END";
    private AtmosphereClient ws;
    private boolean ownRequestPending;

    private AtmosphereListener _cb = new AtmosphereListener() {

        StringBuilder msg = new StringBuilder();
        boolean msgOpen;
        private int msgIndex;

        public void onConnected(int heartbeat, int connectionID) {
            VConsole.log("WS Connected");
            if (!applicationRunning) {
                repaintAll();
            } else if (visitServerOnConnect) {
                // reconnecting, ensure we get sane answers and up to date state
                sendPendingVariableChanges();
            } else {
                getConnectionGuard().connected();
            }
        }

        public void onBeforeDisconnected() {
            VConsole.log("DEBUG: onbeforediconnected");
        }

        public void onDisconnected() {
            VConsole.error("WS Disconnected");
            // reconnect is automatic
            // TODO I guess this should be configurable for different use cases
            // TODO how to handle windows forgotten open ?
            getConnectionGuard().disconnected();
        }

        public void onError(Throwable exception, boolean connected) {
            VConsole.log("DEBUG: onError");
            VConsole.log(exception);
            getConnectionGuard().errorOccured();
        }

        public void onHeartbeat() {
            VConsole.log("DEBUG: onHeartbeat");
        }

        public void onRefresh() {
            VConsole.log("DEBUG: onRefresh");
        }

        public void onAfterRefresh() {
            VConsole.log("DEBUG: onAfterRefresh");
            getConnectionGuard().connected();
        }

        public void onMessage(List<?> messages) {
            for (Object o : messages) {
                String message = o.toString().replace("@NL@", "\\n");
                VConsole.log("message|" + message + "|");

                final Date start = new Date();
                if (!msgOpen) {
                    msg.append("{");
                    msgOpen = true;
                }
                msg.append(message);
                boolean terminated = false;
                if (message.endsWith("D")) {
                    String string = msg.toString();
                    if (string.endsWith(MSG_TERMINATION_STRING)) {
                        message = string.substring(0, msg.length()
                                - MSG_TERMINATION_STRING.length());
                        terminated = true;
                    }
                }
                if (terminated) {
                    if (!ownRequestPending) {
                        startRequest();
                        VConsole.log("Changeset pushed by the server");
                    }
                    // TODO add some sort of sequence number so we could check
                    // whether this was really anwer to out request or message
                    // pushed by server
                    ownRequestPending = false;
                    message += "}";
                    msg = new StringBuilder();
                    msgOpen = false;
                    try {
                        VConsole.log("Received socket message...");
                        ValueMap json = evaluateUIDL(message);

                        int msgIdx = json.getInt("i");
                        if (msgIdx == 0) {
                            this.msgIndex = msgIdx;
                        } else {
                            this.msgIndex++;
                            if (this.msgIndex != msgIdx) {
                                // If we have missed a message for some reason,
                                // ingore this changeset and repaint the whole
                                // view
                                VConsole.error("Missed a message -> repaint all...");
                                endRequest();
                                repaintAll();
                                getConnectionGuard().responseHandled();
                                return;
                            }
                        }

                        if (applicationRunning) {
                            handleUIDLMessage(start, message, json);
                        } else {
                            applicationRunning = true;
                            handleWhenCSSLoaded(message, json);
                            ApplicationConfiguration.startNextApplication();
                        }
                        getConnectionGuard().responseHandled();
                    } catch (Exception e) {
                        VConsole.log("Received socket message, but parsing failed!");
                        VConsole.log(message);
                        VConsole.log(e);
                        endRequest();
                        getConnectionGuard().parsingErrorOccured();
                    }
                }
            }
        }
    };

    private ConnectionGuard errorDisplay;
    private boolean visitServerOnConnect;

    // This is needed just to make IE/Opera work
    private AtmosphereGWTSerializer serializer = new AtmosphereGWTSerializer() {

        @Override
        public String serialize(Object message) throws SerializationException {
            return message.toString();
        }

        @Override
        protected Serializer getRPCSerializer() {
            return null;
        }

        @Override
        public SerialMode getPushMode() {
            return SerialMode.PLAIN;
        }

        @Override
        public SerialMode getMode() {
            return SerialMode.PLAIN;
        }

        @Override
        public Object deserialize(String message) throws SerializationException {
            return message;
        }
    };

    @Override
    public void init(WidgetSet widgetSet, ApplicationConfiguration cnf) {
        super.init(widgetSet, cnf);
        // First opening of WS will repaint all -> start the app
        getWebSocket();
    }

    private ConnectionGuard getConnectionGuard() {
        if (errorDisplay == null) {
            errorDisplay = GWT.create(ConnectionGuard.class);
            errorDisplay.setSocketApplicationConnection(this);
        }
        return errorDisplay;
    }

    private AtmosphereClient getWebSocket() {
        if (this.ws == null) {
            // if timed out or not started, create websocket to server
            String url = getConfiguration().getApplicationUri() + "UIDL/";
            if (url.startsWith("/")) {
                String hostPageBaseURL = GWT.getHostPageBaseURL();
                String[] split = hostPageBaseURL.split("\\/\\/");
                String host = split[1].substring(0, split[1].indexOf("/"));
                String protoAndHost = split[0] + "//" + host;
                url = protoAndHost + url;
            }

            url += getConfiguration().getInitialWindowName();
            url += "?w="+getConfiguration().getInitialWindowName()+"&cmid="+getCmId(getConfiguration().getRootPanelId());
            VConsole.log(url);

            VConsole.log("Creating atmosphere client...");
            this.ws = new AtmosphereClient(url, serializer, _cb, true);
            VConsole.log("...starting...");

            this.ws.start();
        }
        return this.ws;
    }

    private final native String getCmId(String string)
    /*-{
        return $wnd.vaadin.vaadinConfigurations[string]['cmid'];
    }-*/;

    @Override
    public void start() {
        VConsole.log("No real start here");
        /*
         * NOP, init when web sockect connected
         */
    }

    @Override
    protected void makeUidlRequest(String requestData, String extraParams,
            boolean forceSync) {
        if (visitServerOnConnect) {
            // TODO atmosphere should keep track of unsent changes for us ? GWT
            // atmosphere or our usage issue? Now repainting to make sure state
            // is in sync, but that may be bit expensive in some cases.
            visitServerOnConnect = false;
            extraParams = "repaintAll=1";
        }
        VConsole.log("->SERVER: " + requestData + "; p: " + extraParams
                + "; forceSync: " + forceSync);

        // Due to atmosphere bug/feature/whatever we need to urlencode the
        // payload as well (linebreaks are not allowed in messages)
        requestData = URL.encodeQueryString(requestData);
        if (forceSync) {
            /*
             * TODO Check if this really works. Else we could send the last
             * "window close event" with synchronous xhr.
             */
            getWebSocket().broadcast(extraParams + "#" + requestData);
        } else {
            startRequest();
            this.ownRequestPending = true;
            getConnectionGuard().expectResponse();
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

    public void goOffline() {
        ws.stop();
        if (ownRequestPending) {
            endRequest();
            ownRequestPending = false;
        }
    }

    public void reconnect() {
        visitServerOnConnect = true;
        ws.start();
    }

    public void restart() {
        String appUri = getAppUri() + "?restartApplication";
        Window.Location.replace(appUri);
    }

    @Override
    public void sendPendingVariableChanges() {
        // leave stuff in queue if no active connection
        if (applicationRunning && ws != null && ws.isRunning()) {
            super.sendPendingVariableChanges();
        }
    }

}
