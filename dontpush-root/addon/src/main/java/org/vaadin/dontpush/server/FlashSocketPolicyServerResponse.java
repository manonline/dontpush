package org.vaadin.dontpush.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

class FlashSocketPolicyServerResponse extends Thread {
	private Socket socket;

	public FlashSocketPolicyServerResponse(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		try {
			BufferedReader socketIn = new BufferedReader(new InputStreamReader(
					this.socket.getInputStream()));
			char[] in = new char[FlashSocketPolicyServer.POLICY_REQUEST.length()];
			int read = socketIn.read(in);
			String readLine = new String(in);
			if (FlashSocketPolicyServer.POLICY_REQUEST.equals(readLine)) {
				PrintWriter printWriter = new PrintWriter(
						this.socket.getOutputStream(), true);
				printWriter
						.write(FlashSocketPolicyServer.POLICY_XML + "\u0000");
				printWriter.close();
				Logger.getAnonymousLogger().fine("Policy responded to :" + socket.getRemoteSocketAddress());
				System.out.println();
			}
		} catch (Exception e) {
			Logger.getAnonymousLogger().log(Level.WARNING, "policy response failed", e);
		}
	}
}