package org.vaadin.dontpush.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * TODO modify this grant access only to the right port
 */
public class FlashSocketPolicyServer extends Thread {
	
	public static void main(String[] args) {
		new FlashSocketPolicyServer().start();
	}
	
	public static final String POLICY_REQUEST = "<policy-file-request/>";
	public static final String POLICY_XML = "<?xml version=\"1.0\"?>"
			+ "<cross-domain-policy>"
			+ "<allow-access-from domain=\"*\" to-ports=\"*\" />"
			+ "</cross-domain-policy>";

	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(843);
			Logger.getAnonymousLogger().info("Policy server running");
			while (true) {
				Socket socket = serverSocket.accept();
				Logger.getAnonymousLogger().info("Policy request received");
				FlashSocketPolicyServerResponse tread = new FlashSocketPolicyServerResponse(
						socket);
				tread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}