package org.vaadin.dontpush.demo;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.Application;
import com.vaadin.service.ApplicationContext.TransactionListener;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.Window;

public class DontpushApplicationWithTrxListeners extends Application implements HttpServletRequestListener {
	
	@Override
	public void init() {
		TestWindow testWindow = new TestWindow();
		setMainWindow(testWindow);
		getContext().addTransactionListener(new TransactionListener() {
			
			@Override
			public void transactionStart(Application application, Object transactionData) {
				System.err.println("trx start");
			}
			
			@Override
			public void transactionEnd(Application application, Object transactionData) {
				System.err.println("trx end");
			}
		});
	}
	
	@Override
	public Window getWindow(String name) {
		Window w = super.getWindow(name);
		if(w == null) {
			w = new TestWindow();
			addWindow(w);
			w.open(new ExternalResource(w.getURL()));
			return w;
		}
		return w;
	}
	
	@Override
	public void close() {
		super.close();
		Collection<Window> windows2 = getWindows();
		for (Window window : windows2) {
			TestWindow.unregister((TestWindow) window);
		}
	}

	@Override
	public void onRequestStart(HttpServletRequest request,
			HttpServletResponse response) {
		System.err.println("request start");
	}

	@Override
	public void onRequestEnd(HttpServletRequest request,
			HttpServletResponse response) {
		System.err.println("request end");
	}
	
	
	

	

}
