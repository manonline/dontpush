/*
   Copyright [2010] [Vaadin Ltd]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.vaadin.dontpush.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.Application;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Response;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.ui.Window;

/**
 * Servlet that can upgrade request to websockets for more efficient client
 * server communication.
 * <p>
 * TODO check if here is something generic for abstract super class among
 * various app servers. Or better yet, detect if we could use same servlet for
 * all servers and create app server specific parts dynamically.
 * 
 * @author mattitahvonen
 */
public class DontPushOzoneServlet extends ApplicationServlet  {

	private HttpServletRequest request;

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO use Meteor instead? Works with websockets?
		this.request = request;
		super.service(request, response);
		this.request = null;
	}


	@Override
	public CommunicationManager createCommunicationManager(
			final Application application) {
		/*
		 * TODO get rid of session attribute if using meteor servlet works.
		 */
		SocketCommunicationManager socketCommunicationManager = new SocketCommunicationManager(application);
		request.getSession().setAttribute(SocketCommunicationManager.class.getName(), socketCommunicationManager);
		return socketCommunicationManager;
	}

}
