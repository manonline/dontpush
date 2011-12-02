package org.vaadin.dontpush.demo;

import java.util.Date;
import java.util.HashSet;

import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;


public class TestWindow extends Window {
	public static HashSet<TestWindow> openApps = new HashSet<TestWindow>();

	private CssLayout messages;

	public TestWindow() {

		Label label = new Label("Hello Vaadin user. This is a demo app for Atmosphere powered DontPush implementation.");
		addComponent(label);
        final ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setValue(0);

        /*
         * Don't poll, don't push.
         */
        progressIndicator.setPollingInterval(9990000);

        addComponent(progressIndicator);

        Button b = new Button("a Button");
        addComponent(b);
        b.addListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                event.getButton().getWindow().showNotification("clicked");
            }
        });

        Thread thread = new Thread() {
            float f = 0;

            @Override
            public void run() {
                while (true) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    f += 0.05;
                    synchronized (getApplication()) {
                        progressIndicator.setValue(new Float(f));
                    }
                    if (f > 1) {
                        break;
                    }
                    // Don't push, the magic will just happen.
                }
            }
        };

        thread.start();
        
        messages = new CssLayout();
        messages.setWidth("600px");
        
        final TextField textField = new TextField("Post message");
        
        Button button = new Button("Post to all users");
        
        button.addListener(new Button.ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				String msg = (String) textField.getValue();
				broadcast(msg);
			}
		});
        
        addComponent(textField);
        addComponent(button);
        addComponent(messages);
        
		
	}
	
	@Override
	public void attach() {
		super.attach();
        register(this);
	}
	
	@Override
	public void detach() {
		super.detach();
		unregister(this);
	}
	
	static void register(TestWindow app) {
		synchronized (openApps) {
			openApps.add(app);
		}
	}
	
	static void unregister(TestWindow app) {
		synchronized (openApps) {
			openApps.remove(app);
		}
	}
	
	static void broadcast(String msg) {
		TestWindow[] apps;
		synchronized (openApps) {
			apps = new TestWindow[openApps.size()];
			openApps.toArray(apps);
		}
		for (int i = 0; i < apps.length; i++) {
			TestWindow app = apps[i];
			synchronized (app) {
				app.addMessage(msg);
			}
		}
	}


	private void addMessage(String msg) {
		messages.addComponent(new Label(new Date() + " New message:" + msg));
		if(messages.getComponentCount() > 4) {
			messages.removeComponent(messages.getComponentIterator().next());
		}
	}


}
