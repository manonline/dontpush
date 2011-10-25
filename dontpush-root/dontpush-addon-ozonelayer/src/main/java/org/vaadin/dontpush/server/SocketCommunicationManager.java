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

import com.vaadin.Application;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.Paintable.RepaintRequestEvent;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mattitahvonen
 */
public class SocketCommunicationManager extends CommunicationManager {

    public SocketCommunicationManager(Application application) {
        super(application);
    }

    @Override
    public Application getApplication() {
        return super.getApplication();
    }

    private boolean uidlRequest;
    private Map<Window, VaadinWebSocket> windowToSocket = new HashMap<Window, VaadinWebSocket>();

    @Override
    public boolean handleVariableBurst(Object source, Application app,
            boolean success, String burst) {
        uidlRequest = true;
        try {
            return super.handleVariableBurst(source, app, success, burst);
        } finally {
            uidlRequest = false;
        }
    }

    @Override
    public void repaintRequested(RepaintRequestEvent event) {
        super.repaintRequested(event);
        Component paintable = (Component) event.getPaintable();
        Window window = paintable.getWindow();
        if (window.getParent() != null) {
            window = window.getParent();
        }
        if (!uidlRequest) {
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
                }
                synchronized (getApplication()) {
                    // // TODO Optimization Conditionally paint if still dirty (eg. client
                    // requst may have rendered dirty paintables)
                    try {
                        getSocketForWindow(window).paintChanges(false, false);
                    } catch (PaintException e) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Paint failed", e);
                    } catch (IOException e) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Paint failed (IO)", e);
                    }
                }
            }

        };
        thread.start();
    }

    private VaadinWebSocket getSocketForWindow(Window window) {
        return windowToSocket.get(window);
    }

    public void setSocket(VaadinWebSocket vaadinWebSocket, Window window) {
        windowToSocket.put(window, vaadinWebSocket);
    }

}
