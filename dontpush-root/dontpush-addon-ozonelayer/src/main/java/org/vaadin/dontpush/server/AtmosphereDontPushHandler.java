package org.vaadin.dontpush.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;
import org.atmosphere.gwt.server.GwtAtmosphereResource;

import com.vaadin.ui.Window;

public class AtmosphereDontPushHandler extends AtmosphereGwtHandler {

	@Override
	public void broadcast(Serializable message, GwtAtmosphereResource resource) {
		BroadCasterVaadinSocket socket = resource
				.getAttribute(BroadCasterVaadinSocket.class.getName());
		String data = message.toString();
		socket.handlePayload(data);
	}

	@Override
	public int doComet(GwtAtmosphereResource resource) throws ServletException,
			IOException {

		/*
		 * TODO expect problems here. Session, websocket grizzly ~ nogo or
		 * athmosphere does some magic i don't know about. Prepare to connect to
		 * session by request path
		 */
		HttpSession session = resource.getSession(false);
		if (session != null) {
			/*
			 * TODO clean broadcasters when session dies 
			 * 
			 * TODO check and handle
			 * possible timing issues when renewing the "Socket" with long
			 * polling. Currently changes can get lost if server side change exactly when socket is renewed?
			 */

			String path = resource.getRequest().getPathInfo();
			String windowName = path.substring(path.lastIndexOf("/"));
			Broadcaster bc = DefaultBroadcasterFactory.getDefault().lookup(
					DefaultBroadcaster.class,
					"dontpush-" + session.getId() + "-" + windowName, true);
			resource.getAtmosphereResource().setBroadcaster(bc);

			SocketCommunicationManager cm = (SocketCommunicationManager) session
					.getAttribute(SocketCommunicationManager.class.getName());
			Window window;
			if ("/null".equals(windowName)) {
				window = cm.getApplication().getMainWindow();
			} else {
				window = cm.getApplication().getWindow(windowName);
			}
			BroadCasterVaadinSocket socket = new BroadCasterVaadinSocket(
					resource, cm, window);
			resource.setAttribute(BroadCasterVaadinSocket.class.getName(),
					socket);
			cm.setSocket(socket, window);
			Logger.getLogger(getClass().getName()).fine("doComet: Connected to CM" + session.getId());
		}

		return NO_TIMEOUT;
	}

	@Override
	public void doPost(List<Serializable> messages, GwtAtmosphereResource r) {
		Logger.getLogger(getClass().getName()).severe(
				"TODO Never happens in our case?");
	}

}
