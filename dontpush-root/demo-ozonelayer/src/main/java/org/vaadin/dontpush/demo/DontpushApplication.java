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
			if(name.startsWith("org.vaadin")) {
				try {
					Class<?> forName = Class.forName(name);
					try {
						Window newInstance = (Window) forName.newInstance();
						w = newInstance;
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(w == null) {
				w = new TestWindow();
			}
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
