package org.vaadin.dontpush.demo.tests;

import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

public class TestMessageSize extends Window {
	
	public TestMessageSize() {
		
		final TextField feed = new TextField();
		feed.setValue("dirtdirtdirt");
		addComponent(feed);
		
		final TextField txtlenght = new TextField();
		txtlenght.setValue("7020");
		addComponent(txtlenght);
		Button button = new Button("Created dirt");
		
		addComponent(button);
		
		final Label l = new Label("Dirt");
		addComponent(l);
		
		button.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				int lenght = Integer.parseInt(txtlenght.getValue().toString());
				StringBuilder sb = new StringBuilder();
				sb.append(feed.getValue().toString());
				while(sb.length() < lenght) {
					sb.append(sb.toString());
				}
				l.setValue(sb.substring(0, lenght));
			}
		});
		
	}

}
