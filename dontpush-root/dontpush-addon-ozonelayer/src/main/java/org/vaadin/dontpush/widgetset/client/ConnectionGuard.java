package org.vaadin.dontpush.widgetset.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ui.VOverlay;

public class ConnectionGuard extends VOverlay {

    // TODO make these messages configurable
    private static final String DISMISS_TITLE = "Just hide this message and ignore the problem.";
    private static final String RELOAD_TITLE = "Reloads the page and restarts the app with new app session.";
    private static final String RECONNECT_TITLE = "Enforces the client to try reconnecting to server";
    private static final String DISMISS = "Dismiss";
    private static final String RESTART_APP = "Restart app";
    private static final String RECONNECT = "Reconnect";
    private static final String MSG_DEFAULT = "<h1>Ooops..</h1><p>Connection to server appears to be broken. This may be due to a temporary network problem or the server has gone is offline or very busy. The connection may return automatically or you may try to reconnect/restart your app.</p>";
    private static final String MSG_BADRESPONSE = "<h1>Ooops..</h1><p>Coudn't parse the message from the server. The service may be down or your session is dead. Restart is required :-(</p>";

    private Timer t = new Timer() {
        @Override
        public void run() {
            show();
        }

        @Override
        public void cancel() {
            super.cancel();
            showingOrAboutToShow = false;
        };
    };

    private static final int DEFAULT_TIMEOUT = 5000;

    private SocketApplicationConnection ac;

    private boolean expectingResponse;

    private HTML html;
    private Button reconnect;
    private Button restart;
    private Button ignore;
    private int timeout = -1;
    private boolean showingOrAboutToShow;

    public ConnectionGuard() {

        setAnimationEnabled(true);
        setAutoHideEnabled(false);
        addStyleName("v-Notification v-Notification-error ozonelayer-guard");
        FlowPanel flowPanel = new FlowPanel();
        html = new HTML();
        flowPanel.add(html);
        setMessage(MSG_DEFAULT);

        reconnect = new Button(RECONNECT);
        reconnect.setTitle(RECONNECT_TITLE);
        flowPanel.add(reconnect);
        reconnect.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                expectResponse();
                ac.reconnect();
            }
        });

        restart = new Button(RESTART_APP);
        restart.setTitle(RELOAD_TITLE);
        flowPanel.add(restart);
        restart.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                ac.restart();
            }
        });
        ignore = new Button(DISMISS);
        ignore.setTitle(DISMISS_TITLE);
        flowPanel.add(ignore);
        ignore.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                hide();
            }
        });
        add(flowPanel);
    }

    private void setMessage(String default2) {
        html.setHTML(default2);
    }

    public void expectResponse() {
        if (!expectingResponse) {
            t.schedule(getConnectionGuardTimeout());
            expectingResponse = true;
        }
    }

    /**
     * @return the time that {@link ConnectionGuard} lets waits for connection
     *         to return or response to receive.
     */
    private int getConnectionGuardTimeout() {
        if (timeout == -1) {
            timeout = readFromPage() * 1000;
            if (timeout < 0) {
                timeout = DEFAULT_TIMEOUT;
            } else {
                VConsole.log("Custom ozonelayerConnectionGuardTimeout timeout value"
                        + timeout);
            }
        }
        return timeout;
    }

    private static native int readFromPage()
    /*-{
        if($wnd.ozonelayerConnectionGuardTimeout) {
            return $wnd.ozonelayerConnectionGuardTimeout;
        }
        return -1;
    }-*/;

    public void responseHandled() {
        expectingResponse = false;
        t.cancel();
        if (isShowing()) {
            hide();
        }
    }

    public void disconnected() {
        if (!showingOrAboutToShow) {
            t.schedule(getConnectionGuardTimeout());
            showingOrAboutToShow = true;
        }
    }

    public void connected() {
        t.cancel();
    }

    public void setSocketApplicationConnection(
            SocketApplicationConnection socketApplicationConnection) {
        this.ac = socketApplicationConnection;
    }

    @Override
    public void show() {
        super.show();
        ac.goOffline();
    }

    public void errorOccured() {
        // TODO Could be more eager handling here
        disconnected();
    }

    public void parsingErrorOccured() {
        setMessage(MSG_BADRESPONSE);
        reconnect.setVisible(false);
    }

    @Override
    public void hide() {
        super.hide();
        showingOrAboutToShow = false;
    }

}
