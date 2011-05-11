package org.vaadin.dontpush.server;

import java.io.IOException;

import com.vaadin.terminal.PaintException;

/**
 * Interface for SocketCommunicationManager. Each app server adapter needs an implementation of this.
 * 
 * @author mattitahvonen
 */
public interface VaadinWebSocket {

	void paintChanges(boolean repaintAll, boolean analyzeLayouts) throws PaintException, IOException;

}
