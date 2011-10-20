package org.vaadin.dontpush.demo;

import java.util.Date;
import java.util.HashSet;

import com.vaadin.Application;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

public class DontpushApplication extends Application {
	
	public static HashSet<DontpushApplication> openApps = new HashSet<DontpushApplication>();
	
	private Label label2;
	private CssLayout messages;

	@Override
	public void init() {
		Window main = new Window("Dontpush Application");
		Label label = new Label("Hello Vaadin user");
		main.addComponent(label);
		setMainWindow(main);
        final ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setValue(0);

        /*
         * Don't poll, don't push.
         */
        progressIndicator.setPollingInterval(9990000);

        main.addComponent(progressIndicator);

        Button b = new Button("a Button");
        main.addComponent(b);
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
                    synchronized (DontpushApplication.this) {
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
			
			@Override
			public void buttonClick(ClickEvent event) {
				String msg = (String) textField.getValue();
				broadcast(msg);
			}
		});
        
        main.addComponent(textField);
        main.addComponent(button);
        main.addComponent(messages);
        
        register(this);

	}
	
	
	@Override
	public void close() {
		super.close();
		unregister(this);
	}
	
	static void register(DontpushApplication app) {
		synchronized (openApps) {
			openApps.add(app);
		}
	}
	
	static void unregister(DontpushApplication app) {
		synchronized (openApps) {
			openApps.remove(app);
		}
	}
	
	static void broadcast(String msg) {
		DontpushApplication[] apps;
		synchronized (openApps) {
			apps = new DontpushApplication[openApps.size()];
			openApps.toArray(apps);
		}
		for (int i = 0; i < apps.length; i++) {
			DontpushApplication app = apps[i];
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
