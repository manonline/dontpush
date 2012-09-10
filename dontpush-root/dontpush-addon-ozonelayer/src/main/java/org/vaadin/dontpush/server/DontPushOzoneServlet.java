/*
   Copyright [2011] [Vaadin Ltd]

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

import com.vaadin.Application;
import com.vaadin.service.ApplicationContext;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.ui.Window;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet that can upgrade request to websockets for more efficient client
 * server communication.
 * <p>
 * TODO check if here is something generic for abstract super class among
 * various app servers. Or better yet, detect if we could use same servlet for
 * all servers and create app server specific parts dynamically.
 *
 * @author mattitahvonen
 * @author Mark Thomas
 */
@SuppressWarnings("serial")
public class DontPushOzoneServlet extends ApplicationServlet {

    private Class<? extends SocketCommunicationManager> communicationManagerClass;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String communicationManagerClassName = config
                .getInitParameter("communicationManagerClass");
        if (communicationManagerClassName != null) {
            try {
                this.communicationManagerClass = (Class<SocketCommunicationManager>) Class
                        .forName(communicationManagerClassName);
            } catch (ClassNotFoundException e) {
                this.communicationManagerClass = null;
            }
        }
    }

    @Override
    protected WebApplicationContext getApplicationContext(HttpSession session) {
        WebApplicationContext cx = (WebApplicationContext) session
                .getAttribute(WebApplicationContext.class.getName());
        if (cx == null) {
            cx = new DontPushOzoneWebApplicationContext(session,
                    this.communicationManagerClass);
            session.setAttribute(WebApplicationContext.class.getName(), cx);
        }
        return cx;
    }
    
    @Override
    protected void writeAjaxPageHtmlVaadinScripts(Window window,
            String themeName, Application application, BufferedWriter page,
            String appUrl, String themeUri, String appId,
            HttpServletRequest request) throws ServletException, IOException {
        super.writeAjaxPageHtmlVaadinScripts(window, themeName, application, page,
                appUrl, themeUri, appId, request);
        DontPushOzoneWebApplicationContext context = (DontPushOzoneWebApplicationContext) application.getContext();
        SocketCommunicationManager applicationManager = (SocketCommunicationManager) context.getApplicationManager(application, this);
        String id = applicationManager.getId();
        page.write("<script type='text/javascript'>vaadin.vaadinConfigurations['"+appId+"'].cmid = '"+id+"';</script>");
    }

    @Override
    protected void writeAjaxPageHtmlHeader(BufferedWriter page, String title,
            String themeUri, HttpServletRequest request) throws IOException {
        super.writeAjaxPageHtmlHeader(page, title, themeUri, request);
        String cGuardTimeout = getApplicationProperty("connectionGuardTimeout");
        if (cGuardTimeout != null) {
            int to = Integer.parseInt(cGuardTimeout);
            page.write("<script type=\"text/javascript\">ozonelayerConnectionGuardTimeout = "
                    + to + ";</script>");
        }
    }

}
