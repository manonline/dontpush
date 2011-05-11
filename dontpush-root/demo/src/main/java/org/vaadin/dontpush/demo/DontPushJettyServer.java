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

package org.vaadin.dontpush.demo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.vaadin.dontpush.server.FlashSocketPolicyServer;

/**
 * TODO clean this, most of this is not needed for DontPush
 * 
 * Class for running Jetty servlet container within Eclipse project.
 */
public class DontPushJettyServer {

	private final static int serverPort = 8888;

	/**
	 * Server to start embedded jetty and a simple flash socket policy server.
	 * As the flash socket polycy server need a privileged port, this should be
	 * started with root privileges.
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static void main(String[] args) throws InterruptedException {
		// Start Jetty
		System.out.println("Starting Jetty...");
		Server startJetty = startJetty();
		startFlashSocketPolycyServer();
		startJetty.join();
	}

	private static void startFlashSocketPolycyServer() {
		try {
			/*
			 * Run a server on port 843 to allow flash fallback to work
			 * 
			 * Get rid of this once jetty has a support for this:
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=298718
			 */
			FlashSocketPolicyServer flashSocketPolicyServer = new FlashSocketPolicyServer();
			flashSocketPolicyServer.start();

		} catch (Exception e) {
			System.out.println("Not root or flash policy server "
					+ "already running in port 843");

		}
	}

	private static Server startJetty() {
		Server server = new Server();

		final Connector connector = new SelectChannelConnector();

		connector.setPort(serverPort);
		server.setConnectors(new Connector[] { connector });

		WebAppContext context = new WebAppContext();
		context.setDescriptor("./src/main/webapp/WEB-INF/web.xml");
		File file = new File("./target");
		File[] listFiles = file.listFiles();
		for (File file2 : listFiles) {
			if (file2.isDirectory()
					&& file2.getName().startsWith("dontpush-demo-")) {
				context.setWar(file2.getPath());
				break;
			}
		}
		context.setContextPath("/");

		server.setHandler(context);

		try {
			server.start();
		} catch (Exception e) {
			try {
				server.stop();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return server;
	}

}
