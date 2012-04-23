package org.vaadin.dontpush.demo.tests;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

public class BrowserDetails extends Window {

	public BrowserDetails() {
		final Button b = new Button("Refresh details (client side details may appear on new sessions)");
		addComponent(b);
		
		b.addListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				removeAllComponents();
				addComponent(b);
				printWebBrowserDetails();
			}
		});

	}

	@Override
	public void attach() {
		super.attach();
		printWebBrowserDetails();
		
	}

	private void printWebBrowserDetails() {
		WebBrowser browser = ((WebApplicationContext) getApplication()
				.getContext()).getBrowser();
		
		addComponent(new Label("Address (request detail)" + browser.getAddress()));
		addComponent(new Label("Browser (request detail)" + browser.getBrowserApplication()));
		addComponent(new Label("Screen width (client side detail) " + browser.getScreenWidth()));
	}

}
