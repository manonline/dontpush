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

import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Response;
import com.vaadin.ui.Window;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.atmosphere.gwt.server.GwtAtmosphereResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcasterVaadinSocket implements VaadinWebSocket {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected GwtAtmosphereResource resource;
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

    public BroadcasterVaadinSocket(GwtAtmosphereResource resource, SocketCommunicationManager cm, Window window2) {
        this.resource = resource;
        this.cm = cm;
        this.window = window2;
    }

    public void paintChanges(boolean repaintAll, boolean analyzeLayouts) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter out = null;
        try {
            out = new PrintWriter(os);
            this.cm.writeUidlResponce(this.callBack, repaintAll, out, this.window, analyzeLayouts);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        this.resource.post(new String(os.toByteArray()));
    }

    public void handlePayload(String data) {
        String[] split = data.split("#");
        String params = split[0];
        boolean repaintAll = params.contains("repaintAll");
        if (repaintAll) {
            this.cm.makeAllPaintablesDirty(this.window);
        }
        boolean analyzeLayouts = params.contains("analyzeLayouts");
        // TODO handle various special variables (request params in std xhr)
        boolean success = true;
        if (split.length > 1) {
            success = this.cm.handleVariableBurst(this, cm.getApplication(), true, (split.length > 1) ? split[1] : "");
        } else {
            this.cm.makeAllPaintablesDirty(this.window);
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
