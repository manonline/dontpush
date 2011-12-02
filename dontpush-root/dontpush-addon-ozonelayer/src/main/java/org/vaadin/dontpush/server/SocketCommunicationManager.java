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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.Application;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.Paintable.RepaintRequestEvent;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

/**
 * @author mattitahvonen
 * @author Mark Thomas
 */
public class SocketCommunicationManager extends CommunicationManager {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    private boolean uidlRequest;
    private final transient Map<Window, VaadinWebSocket> windowToSocket = new HashMap<Window, VaadinWebSocket>();
    private String id;

    public SocketCommunicationManager(Application application) {
        super(application);
        id = UUID.randomUUID().toString();
    }
    
    public String getId() {
        return id;
    }

    @Override
    public Application getApplication() {
        return super.getApplication();
    }

    @Override
    public boolean handleVariableBurst(Object source, Application app, boolean success, String burst) {
        this.uidlRequest = true;
        try {
            return super.handleVariableBurst(source, app, success, burst);
        } finally {
            this.uidlRequest = false;
        }
    }

    @Override
    public void repaintRequested(RepaintRequestEvent event) {
        super.repaintRequested(event);
        Component paintable = (Component)event.getPaintable();
        Window window = paintable.getWindow();
        if (window.getParent() != null) {
            window = window.getParent();
        }
        if (!this.uidlRequest) {
            deferPaintPhase(window);
        }
    }

    private void deferPaintPhase(final Window window) {
        Thread thread = new Thread() {
            /**
             * Add a very small latency for the tread that triggers to paint
             * phase.
             *
             * TODO redesign the whole server side paint phase triggering.
             * Probably the best if just a one thread that fires paints for app
             * instances. NOTE that atmosphere may actually do some cool things
             * for us alreay. This may actually be obsolete in atmosphere version.
             */
            private long RESPONSE_LATENCY = 3;

            @Override
            public void run() {
                try {
                    sleep(RESPONSE_LATENCY);
                } catch (InterruptedException e) {
                    //ignore
                }
                paintChanges(window);

            }
        };
        thread.start();
    }

    protected void paintChanges(Window window) {
        synchronized (getApplication()) {
            try {
                getSocketForWindow(window).paintChanges(false, false);
            } catch (PaintException e) {
                this.logger.error("Paint failed", e);
            } catch (IOException e) {
                this.logger.error("Paint failed (IO)", e);
            }
        }
    }
    
    protected VaadinWebSocket getSocketForWindow(Window window) {
        return windowToSocket.get(window);
    }

    public void setSocket(VaadinWebSocket vaadinWebSocket, Window window) {
        windowToSocket.put(window, vaadinWebSocket);
    }

}
