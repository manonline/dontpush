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
package org.vaadin.dontpush.server;

import java.io.IOException;

import com.vaadin.Application;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.Paintable.RepaintRequestEvent;
import com.vaadin.terminal.gwt.server.CommunicationManager;

/**
 * TODO remove Jetty dependency.
 * 
 * @author mattitahvonen
 */
public class SocketCommunicationManager extends CommunicationManager {
	public SocketCommunicationManager(Application application) {
		super(application);
	}

	private boolean uidlRequest;
	private VaadinWebSocket socket;
	
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
		if (!uidlRequest) {
			deferPaintPhase();
		}
	}

	private void deferPaintPhase() {
		Thread thread = new Thread() {
			/**
			 * Add a very small latency for the tread that triggers to paint
			 * phase.
			 * 
			 * TODO redesign the whole server side trigger. Probably the best if
			 * just a one thread that fires paints for app instances.
			 */
			private long RESPONSE_LATENCY = 3;

			@Override
			public void run() {
				try {
					sleep(RESPONSE_LATENCY);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				synchronized (getApplication()) {
					// // TODO Conditionally paint if still dirty (eg. client
					// requst may
					// have rendered dirty paintables)
					System.err.println("Painting changes");
					try {
						socket.paintChanges(false, false);
					} catch (PaintException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}

	public void setSocket(VaadinWebSocket vaadinWebSocket) {
		this.socket = vaadinWebSocket;
	}

}
