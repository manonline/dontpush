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
import com.vaadin.Application.WindowDetachEvent;
import com.vaadin.Application.WindowDetachListener;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.Paintable.RepaintRequestEvent;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mattitahvonen
 * @author Mark Thomas
 */
@SuppressWarnings("serial")
public class SocketCommunicationManager extends CommunicationManager implements
        WindowDetachListener {

    protected final transient Logger logger = LoggerFactory
            .getLogger(getClass());
    private final Map<Window, VaadinWebSocket> windowToSocket = new HashMap<Window, VaadinWebSocket>();
    private String id;
    private Window activeUidlRequestWindow;

    private final Set<Window> dirtyWindows = new HashSet<Window>();
    private Runnable pendingPaint;
    private final Set<Window> detachedwindows = new HashSet<Window>();
    private Executor executor;

    private static Map<String, SocketCommunicationManager> idToMgr = Collections.synchronizedMap(new HashMap<String, SocketCommunicationManager>());

    public SocketCommunicationManager(Application application) {
        super(application);
        id = UUID.randomUUID().toString();
        application.addListener(this);
        idToMgr.put(id, this);
    }

    public String getId() {
        return id;
    }

    @Override
    public Application getApplication() {
        return super.getApplication();
    }

    @Override
    public void repaintRequested(RepaintRequestEvent event) {
        super.repaintRequested(event);
        Component paintable = (Component) event.getPaintable();
        Window window = paintable.getWindow();

        // Handle case where components that are referenced by some
        // other object and have been removed from application receive data.
        // Example: Component 'A' has a Container 'C' that listens to events
        // from some global object 'O' (e.g. singleton Spring bean). 'A' is
        // removed from application but 'C' is still a listener to events from
        // 'O' and 'C' is still the container of 'A' and any repaints of 'C'
        // will fail as it is no longer has a Window as its top-most parent
        //
        // See http://dev.vaadin.com/ticket/8262
        if (window == null)
            return;

        if (window.getParent() != null) {
            window = window.getParent();
        }
        if (this.activeUidlRequestWindow != window) {
            synchronized (this.dirtyWindows) {
                this.dirtyWindows.add(window);
            }
            deferPaintPhase();
        }
    }

    private void deferPaintPhase() {
        if (pendingPaint == null) {
            pendingPaint = new Runnable() {
                @Override
                public void run() {
                    pendingPaint = null;
                    Set<Window> copy;
                    synchronized (dirtyWindows) {
                        copy = new HashSet<Window>(dirtyWindows);
                        dirtyWindows.clear();
                    }
                    for (Window w : copy) {
                        paintChanges(w);
                    }
                }
            };
            getExecutor().execute(pendingPaint);
        }
    }

    private Executor getExecutor() {
        return executor;
    }

    protected void paintChanges(Window window) {
        synchronized (getApplication()) {
            VaadinWebSocket socketForWindow = getSocketForWindow(window);
            if (socketForWindow != null) {
                DontPushOzoneWebApplicationContext context = (DontPushOzoneWebApplicationContext) getApplication()
                        .getContext();
                context.trxStart(getApplication(), getSocketForWindow(window));
                try {
                    socketForWindow.paintChanges(false, false);
                } catch (PaintException e) {
                    this.logger.error("Paint failed", e);
                } catch (IOException e) {
                    this.logger.error("Paint failed (IO)", e);
                } finally {
                    context.trxEnd(getApplication(), getSocketForWindow(window));
                }
            }

        }
    }

    protected VaadinWebSocket getSocketForWindow(Window window) {
        return windowToSocket.get(window);
    }

    public void setSocket(VaadinWebSocket vaadinWebSocket, Window window) {
        windowToSocket.put(window, vaadinWebSocket);
    }

    /**
     * This should be called before changing variables. Otherwise we don't know
     * which window is posting them. Should also be nulled after the change
     */
    protected void setActiveWindow(Window window) {
        activeUidlRequestWindow = window;
    }

    public void destroy() {
        SocketCommunicationManager mgr = idToMgr.remove(id);
        if (mgr != null)
            this.logger.debug("Removed SocketCommunicationManager " + id + " from active map.");
        else
            this.logger.debug("Could not find SocketCommunicationManager " + id + " in active map?");
        cleanDetachedWindows();
        for (Window w : windowToSocket.keySet()) {
            VaadinWebSocket vaadinWebSocket = windowToSocket.get(w);
            vaadinWebSocket.destroy();
        }
    }

    void cleanDetachedWindows() {
        synchronized (this.detachedwindows) {
            if (detachedwindows.isEmpty()) {
                return;
            }
        }
        Set<Window> clone;
        synchronized (this.detachedwindows) {
            clone = new HashSet<Window>(this.detachedwindows);
            this.detachedwindows.clear();
        }
        for (Window w : clone) {
            VaadinWebSocket removed = windowToSocket.remove(w);
            if (removed != null) {
                removed.destroy();
            }
        }
    }

    public void windowDetached(WindowDetachEvent event) {
        // mark for lazy clean up, destroying immediately might disturb e.g.
        // redirects.
        detachedwindows.add(event.getWindow());
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public static SocketCommunicationManager get(String cmId) {
        return idToMgr.get(cmId);
    }
}
