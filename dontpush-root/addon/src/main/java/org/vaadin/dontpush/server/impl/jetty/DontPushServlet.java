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
package org.vaadin.dontpush.server.impl.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.vaadin.dontpush.server.SocketCommunicationManager;
import org.vaadin.dontpush.server.VaadinWebSocket;

import com.vaadin.Application;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Response;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.terminal.gwt.server.SessionExpiredException;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.ui.Window;

/**
 * Servlet that can upgrade request to websockets for more efficient client
 * server communication.
 * <p>
 * TODO check if here is something generic for abstract super class among
 * various app servers. Or better yet, detect if we could use same servlet for
 * all servers and create app server specific parts dynamically.
 * <p>
 * TODO proper logging instead of sysouts
 * 
 * @author mattitahvonen
 */
public class DontPushServlet extends ApplicationServlet implements Callback,
		WebSocketFactory.Acceptor {

	private WebSocketFactory _websocketFactory;

	@Override
	public void init() throws ServletException {
		super.init();
		String bs = getInitParameter("bufferSize");

		_websocketFactory = new WebSocketFactory(this, bs == null ? 8192
				: Integer.parseInt(bs));
		String mit = getInitParameter("maxIdleTime");
		if (mit != null)
			_websocketFactory.setMaxIdleTime(Integer.parseInt(mit));

	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if (_websocketFactory.acceptWebSocket(request, response)
				|| response.isCommitted())
			return;
		super.service(request, response);
	}

	class VaadinWebSocketImpl implements OnTextMessage, VaadinWebSocket {

		private Connection _outbound;
		private Application app;
		private SocketCommunicationManager cm;
		private Window window;

		public VaadinWebSocketImpl(Application existingApplication,
				CommunicationManager applicationManager) {
			this.app = existingApplication;
			this.cm = (SocketCommunicationManager) applicationManager;
			// TODO set real Window, now just supports main w
			window = app.getMainWindow();
			cm.setSocket(this);
		}

		public void paintChanges(boolean repaintAll, boolean analyzeLayouts)
				throws PaintException, IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintWriter out = new PrintWriter(os);
			cm.writeUidlResponce(DontPushServlet.this, repaintAll, out, window,
					analyzeLayouts);
			out.flush();
			out.close();
			_outbound.sendMessage(new String(os.toByteArray()));
		}

		public void onOpen(Connection connection) {
			System.err.println("Connect!?");
			_outbound = connection;
		}

		public void onClose(int closeCode, String message) {
			System.err.println("Socket disconnected");
		}

		public void onMessage(String data) {
			System.err.println("Received message from client:" + data);

			String[] split = data.split("#");
			String params = split[0];
			boolean repaintAll = params.contains("repaintAll");
			if (repaintAll) {
				cm.makeAllPaintablesDirty(window);
			}
			boolean analyzeLayouts = params.contains("analyzeLayouts");
			// TODO handle various special variables (request params in std xhr)
			boolean success = true;
			if (split.length > 1) {
				success = cm.handleVariableBurst(this, app, true,
						(split.length > 1) ? split[1] : "");
			} else {
				//
				cm.makeAllPaintablesDirty(window);
			}

			try {
				if (success) {
					paintChanges(repaintAll, analyzeLayouts);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void criticalNotification(Request request, Response response,
			String cap, String msg, String details, String outOfSyncURL)
			throws IOException {
		this.criticalNotification(request, response, cap, msg, details,
				outOfSyncURL);
	}

	public String getRequestPathInfo(Request request) {
		return this.getRequestPathInfo(request);
	}

	public InputStream getThemeResourceAsStream(String themeName,
			String resource) throws IOException {
		return this.getThemeResourceAsStream(themeName, resource);
	}

	@Override
	public CommunicationManager createCommunicationManager(
			final Application application) {
		return new SocketCommunicationManager(application);
	}

	public WebSocket doWebSocketConnect(HttpServletRequest request,
			String protocol) {
		try {
			Application application = getExistingApplication(request, false);
			WebApplicationContext webApplicationContext = WebApplicationContext
					.getApplicationContext(request.getSession());
			CommunicationManager applicationManager = webApplicationContext
					.getApplicationManager(application, this);
			VaadinWebSocketImpl websocket = new VaadinWebSocketImpl(
					application, applicationManager);

			return websocket;
		} catch (SessionExpiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String checkOrigin(HttpServletRequest request, String host,
			String origin) {
		if (origin == null)
			origin = host;
		return origin;
	}
}
