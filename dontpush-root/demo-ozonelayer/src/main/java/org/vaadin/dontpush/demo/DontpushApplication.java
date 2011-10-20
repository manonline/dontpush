package org.vaadin.dontpush.demo;

import com.vaadin.Application;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

public class DontpushApplication extends Application {
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

	}

}
