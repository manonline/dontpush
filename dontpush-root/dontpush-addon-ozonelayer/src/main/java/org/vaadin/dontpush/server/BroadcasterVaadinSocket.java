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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.atmosphere.cpr.Broadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dontpush.widgetset.client.SocketApplicationConnection;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Response;
import com.vaadin.ui.Window;

public class BroadcasterVaadinSocket implements VaadinWebSocket {

    private static final int MAX_MSG_LENGHT = 1024 * 7; // 8kb WILL FAIL ON
                                                        // WEBSOCKETS !?

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Broadcaster resource;
    protected SocketCommunicationManager cm;

    protected Callback callBack = new Callback() {

        public void criticalNotification(Request request, Response response,
                String cap, String msg, String details, String outOfSyncURL)
                throws IOException {
            // TODO Auto-generated method stub

        }

        public String getRequestPathInfo(Request request) {
            // TODO Auto-generated method stub
            return null;
        }

        public InputStream getThemeResourceAsStream(String themeName,
                String resource) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }
    };

    private Window window;

    public BroadcasterVaadinSocket(Broadcaster resource,
            SocketCommunicationManager cm, Window window2) {
        this.resource = resource;
        this.cm = cm;
        this.window = window2;
    }
    
    /**
     * @return the top level window for which this socket is tied to
     */
    public Window getWindow() {
        return window;
    }

    public void paintChanges(boolean repaintAll, boolean analyzeLayouts)
            throws IOException {
        if (resource.getAtmosphereResources().isEmpty()) {
            logger.debug("No active listeners for window being "
                    + "painted. Skipping paint phase to keep"
                    + " the client in sync.");
            return;
        }

        final Application application = window.getApplication();

        if (application != null && !application.isRunning()) {
            String logoutUrl = application.getLogoutURL();
            if (logoutUrl == null) {
                logoutUrl = application.getURL().toString();
            }
            final String msg = "\"redirect\": {\"url\": \"" + logoutUrl
                    + "\"}" + SocketApplicationConnection.MSG_TERMINATION_STRING;
            this.resource.broadcast(msg);
            return;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter out = null;
        try {
            out = new PrintWriter(os);
            this.cm.writeUidlResponce(this.callBack, repaintAll, out,
                    this.window, analyzeLayouts);
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        } finally {
            if (out != null) {
                out.print(SocketApplicationConnection.MSG_TERMINATION_STRING);
                out.flush();
                out.close();
            }
        }
        byte[] byteArray = os.toByteArray();
        int sent = 0;
        while (sent < byteArray.length) {
            int bufsize = Math.min(byteArray.length - sent, MAX_MSG_LENGHT);
            byte[] buf = new byte[bufsize];
            System.arraycopy(byteArray, sent, buf, 0, bufsize);
            this.resource.broadcast(new String(buf));
            sent += bufsize;
        }
    }

    public void handlePayload(String data) {
        synchronized (cm.getApplication()) {

            final Application app = cm.getApplication();
            DontPushOzoneWebApplicationContext context = (DontPushOzoneWebApplicationContext) app
                    .getContext();
            int paramEnd = data.indexOf("#");
            String params = data.substring(0, paramEnd);
            try {
                String payload = URLDecoder.decode(
                        data.substring(paramEnd + 1), "utf-8");
                boolean repaintAll = params.contains("repaintAll");
                if (repaintAll && params.contains("&sh=")) {
                    updateBrowserProperties(params);
                }
                try {
                    context.trxStart(app, this);

                    if (repaintAll) {
                        this.cm.makeAllPaintablesDirty(this.window);
                    }
                    boolean analyzeLayouts = params.contains("analyzeLayouts");
                    // TODO handle various special variables (request params in
                    // std
                    // xhr)
                    boolean success = true;
                    if (!payload.isEmpty()) {
                        cm.setActiveWindow(window);
                        try {
                            success = this.cm.handleVariableBurst(this,
                                    cm.getApplication(), true, payload);
                        } finally {
                            cm.setActiveWindow(null);
                        }
                    }

                    try {
                        if (success) {
                            paintChanges(repaintAll, analyzeLayouts);
                        }
                    } catch (IOException e) {
                        this.logger.error(e.getMessage(), e);
                    }
                } finally {
                    context.trxEnd(cm.getApplication(), resource);
                }
            } catch (UnsupportedEncodingException e) {
                this.logger.error(e.getMessage(), e);
            }
            cm.cleanDetachedWindows();
        }
    }

    private void updateBrowserProperties(String params) {
        DontPushOzoneWebApplicationContext context = (DontPushOzoneWebApplicationContext) cm
                .getApplication().getContext();
        DontPushWebBrowser browser = context.getBrowser();
        browser.updateClientSideDetails(params);
    }

    public void destroy() {
        resource.destroy();
    }

}
