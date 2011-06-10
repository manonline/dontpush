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

import java.util.Date;

import org.vaadin.dontpush.vwebsocket.client.WebSocket;
import org.vaadin.dontpush.vwebsocket.client.WebSocketListener;

import com.google.gwt.core.client.GWT;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.WidgetSet;

/**
 * Uses WebSockets instead of XHR's for communicating with server.
 * @author mattitahvonen
 */
public class SocketApplicationConnection extends ApplicationConnection {

	private WebSocket ws;

	private WebSocketListener _cb = new WebSocketListener() {

		public void message(String message) {
			if(!ownRequestPending) {
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

		public void disconnected() {
			VConsole.log("WS Disconnected");
			// re-open immediately if timeout happened
			// TODO I guess this should be configurable for different use cases
			// TODO how to handle windows forgotten open ?
			getWebSocket();
		}

		public void connected() {
			VConsole.log("WS Connected");
			if (!applicationRunning) {
				repaintAll();
			}
		}
	};

	private boolean ownRequestPending;

	@Override
	public void init(WidgetSet widgetSet, ApplicationConfiguration cnf) {
		super.init(widgetSet, cnf);
		// First opening of WS will repaint all -> start the app
		getWebSocket();
	}

	private WebSocket getWebSocket() {
		if (ws == null) {
			// if timed out or not started, create websocket to server
			String replaceFirst2 = GWT.getHostPageBaseURL().replaceFirst(
					"http", "ws");
			replaceFirst2 = replaceFirst2.substring(0,
					replaceFirst2.length() - 1);
			replaceFirst2 += getConfiguration().getApplicationUri() + "UIDL";
			VConsole.log(replaceFirst2);
			ws = WebSocket.bind(replaceFirst2);
			ws.setListener(_cb);
		}
		return ws;
	}

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
		VConsole.log("new Socket message: " + requestData);
		if (forceSync) {
			/*
			 * TODO figure out if socket can be used on unload.
			 */
			super.makeUidlRequest(requestData, extraParams, forceSync);
		} else {
			startRequest();
			ownRequestPending = true;
			getWebSocket().send(extraParams + "#" + requestData);
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
