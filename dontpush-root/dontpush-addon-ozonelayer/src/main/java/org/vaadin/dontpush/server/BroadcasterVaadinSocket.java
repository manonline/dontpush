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
import java.io.PrintWriter;

import org.atmosphere.cpr.Broadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcasterVaadinSocket implements VaadinWebSocket {

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

    public void paintChanges(boolean repaintAll, boolean analyzeLayouts)
            throws IOException {
        final Application application = window.getApplication();
        if (!application.isRunning()) {
            String logoutUrl = application.getLogoutURL();
            if (logoutUrl == null) {
                logoutUrl = application.getURL().toString();
            }
            final String msg = "\"redirect\": {\"url\": \"" + logoutUrl + "\"}";
            this.resource.broadcast(msg);
            return;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter out = null;
        try {
            out = new PrintWriter(os);
            this.cm.writeUidlResponce(this.callBack, repaintAll, out,
                    this.window, analyzeLayouts);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        this.resource.broadcast(new String(os.toByteArray()));
    }

    public void handlePayload(String data) {
        synchronized (cm.getApplication()) {

            String[] split = data.split("#");
            String params = (split.length > 0 ? split[0] : "");
            boolean repaintAll = params.contains("repaintAll");
            if (repaintAll) {
                this.cm.makeAllPaintablesDirty(this.window);
            }
            boolean analyzeLayouts = params.contains("analyzeLayouts");
            // TODO handle various special variables (request params in std xhr)
            boolean success = true;
            if (split.length > 1) {
                cm.setActiveWindow(window);
                try {
                    success = this.cm.handleVariableBurst(this, cm.getApplication(), true, split[1]);
                } finally {
                    cm.setActiveWindow(null);
                }
            } else {
                this.cm.makeAllPaintablesDirty(this.window);
                repaintAll = true;
            }

            try {
                if (success) {
                    paintChanges(repaintAll, analyzeLayouts);
                }
            } catch (IOException e) {
                this.logger.error(e.getMessage(), e);
            }
        }
    }

}
