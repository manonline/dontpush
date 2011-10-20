package org.vaadin.dontpush.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.atmosphere.gwt.server.GwtAtmosphereResource;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Response;
import com.vaadin.ui.Window;

public class BroadCasterVaadinSocket implements VaadinWebSocket {

	private GwtAtmosphereResource resource;
	private SocketCommunicationManager cm;
	private Callback callBack = new Callback() {

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

	public BroadCasterVaadinSocket(GwtAtmosphereResource resource, SocketCommunicationManager cm,
			Window window2) {
		this.resource = resource;
		this.cm = cm;
		this.window = window2;
	}

	public void paintChanges(boolean repaintAll, boolean analyzeLayouts)
			throws PaintException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(os);
		cm.writeUidlResponce(callBack, repaintAll, out, window, analyzeLayouts);
		out.flush();
		out.close();
		resource.post(new String(os.toByteArray()));
	}

	public void handlePayload(String data) {
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
			success = cm.handleVariableBurst(this, cm.getApplication(), true,
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
