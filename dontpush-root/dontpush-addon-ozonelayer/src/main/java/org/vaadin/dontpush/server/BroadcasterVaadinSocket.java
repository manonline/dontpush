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

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Response;
import com.vaadin.ui.Window;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.atmosphere.cpr.Broadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dontpush.widgetset.client.SocketApplicationConnection;

public class BroadcasterVaadinSocket implements VaadinWebSocket {

    private static final int MAX_MSG_LENGHT = 1024 * 7; // 8kb WILL FAIL ON
                                                        // WEBSOCKETS !?

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Logger jsonLogger = LoggerFactory
      .getLogger(BroadcasterVaadinSocket.class.getName() + ".JSON");
    protected Broadcaster resource;
    protected SocketCommunicationManager cm;
    private boolean logJSON = Boolean.getBoolean("dontpush.socket.logJSON");

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
    private int msgId;

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

    /**
     * Whether or not we want the JSON being sent to the client to be logged.
     * If enabled, JSON is written to the SLF4J Logger named
     * org.vaadin.dontpush.server.BroadcasterVaadinSocket.JSON, if it exists
     * and only if the Logger has TRACE logging enabled.
     *
     * This value can be turned on by overriding this method to return true,
     * by using the setter, or by adding a boolean system property named
     * "dontpush.socket.logJSON" (e.g. -Ddontpush.socket.logJSON=true)
     */
    public boolean isJSONLoggingEnabled() {
        return this.logJSON;
    }
    public void setJSONLoggingEnabled(boolean logJSON) {
        this.logJSON = logJSON;
    }

    public void paintChanges(boolean repaintAll, boolean analyzeLayouts)
            throws IOException {
        if (resource.getAtmosphereResources().isEmpty()) {
            logger.debug("No active listeners for window being "
                    + "painted. Skipping paint phase to keep"
                    + " the client in sync.");
            return;
        }

        if (!resource.getAtmosphereResources().iterator().next().isSuspended()) {
            logger.debug("Request has been cancelled; cannot paint changes.");
            return;
        }

        final Application application = window.getApplication();

        if (application != null && !application.isRunning()) {
            msgId = 0;
            String logoutUrl = application.getLogoutURL();
            if (logoutUrl == null) {
                logoutUrl = application.getURL().toString();
            }
            final String msg = "\"redirect\": {\"url\": \"" + logoutUrl
                    + "\"}" + getIdxJsonSnippet() + SocketApplicationConnection.MSG_TERMINATION_STRING;
            this.resource.broadcast(msg);
            return;
        }

        if(repaintAll) {
            msgId = 0;
        } else {
            msgId++;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
            this.cm.writeUidlResponce(this.callBack, repaintAll, out,
                    this.window, analyzeLayouts);
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        } finally {
            if (out != null) {
                out.print(getIdxJsonSnippet());
                out.print(SocketApplicationConnection.MSG_TERMINATION_STRING);
                out.flush();
                out.close();
            }
        }
        byte[] byteArray = os.toByteArray();
        int sent = 0;
        while (sent < byteArray.length) {
            int bufsize = Math.min(byteArray.length - sent, MAX_MSG_LENGHT);
            if(sent + bufsize != byteArray.length) {
                bufsize = getValidSplitPoint(sent, bufsize, byteArray);
            }
            byte[] buf = new byte[bufsize];
            System.arraycopy(byteArray, sent, buf, 0, bufsize);
            String str = new String(buf, "UTF-8").replace("\\n", "@NL@");
            this.resource.broadcast(str);
            sent += bufsize;
        }
        if (this.isJSONLoggingEnabled() && this.jsonLogger.isTraceEnabled()) {

            HttpServletRequest req =
              ((HttpServletRequest)resource.getAtmosphereResources()
                .iterator().next().getRequest());
            String ip = "unknown client";
            if (req != null) {
                ip = req.getHeader("X-Forwarded-For");
                if (ip == null)
                    ip = req.getRemoteAddr();
            }
            this.jsonLogger.trace("Sent " + sent + " bytes of JSON to " + ip
              + ":\n" + new String(byteArray));
        }
    }

    private String getIdxJsonSnippet() {
        return ", \"i\":" + msgId;
    }

    private int getValidSplitPoint(int sent, int bufsize, byte[] byteArray) {
        while(true) {
            int lastByte = byteArray[sent + bufsize -1] & 0xff;
            switch (lastByte >>> 6) {
            case 0:
                // normal ascii character, valid break point
                return bufsize;
            case 3:
                // first bit of utf8 extended character, split just before
                return bufsize - 1;
            default:
                // extra bits of utf8 character, go backwards until a valid break point is found
                bufsize--;
                break;
            }
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
                    } catch (Throwable t) {
                        this.logger.error("Error during paint: " + t.getMessage(), t);
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

    @Override
    public void destroy() {
        for (AtmosphereResource res : this.resource.getAtmosphereResources()) {
            if (res.isSuspended() && !res.isResumed() && !res.isCancelled()) {
                res.resume();
            }
        }
    }
}
