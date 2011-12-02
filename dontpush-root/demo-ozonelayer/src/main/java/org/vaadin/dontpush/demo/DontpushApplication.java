package org.vaadin.dontpush.demo;

import java.util.Collection;

import com.vaadin.Application;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Window;

public class DontpushApplication extends Application {
	
	@Override
	public void init() {
		TestWindow testWindow = new TestWindow();
		setMainWindow(testWindow);
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
	
	
	

	

}
