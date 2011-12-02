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

import java.util.Collection;
import java.util.LinkedList;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web application context for Vaadin applications.
 * 
 * This is automatically added as a
 * {@link javax.servlet.http.HttpSessionBindingListener} when added to a
 * {@link javax.servlet.http.HttpSession}.
 * 
 * @author Mark Thomas
 */
public class DontPushOzoneWebApplicationContext extends WebApplicationContext {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Class<SocketCommunicationManager> communicationManagerClass;
    private Collection<SocketCommunicationManager> mgrs = new LinkedList<SocketCommunicationManager>();

    public DontPushOzoneWebApplicationContext(HttpSession session,
            Class<SocketCommunicationManager> communicationManagerClass) {
        super();
        this.session = session;
        this.communicationManagerClass = communicationManagerClass;
    }

    @Override
    public CommunicationManager getApplicationManager(Application application,
            AbstractApplicationServlet servlet) {
        SocketCommunicationManager mgr = (SocketCommunicationManager) this.applicationToAjaxAppMgrMap
                .get(application);

        if (mgr == null) {
            // Creates new manager
            if (this.communicationManagerClass != null) {
                try {
                    mgr = this.communicationManagerClass.getConstructor(
                            Application.class).newInstance(application);
                } catch (Exception e) {
                    this.logger.error(e.getMessage(), e);
                }
            }
            if (mgr == null) {
                mgr = new SocketCommunicationManager(application);
            }
            this.session.setAttribute(
                    SocketCommunicationManager.class.getName(), mgr);
            AtmosphereDontPushHandler.setCommunicationManager(mgr.getId(),
                    mgr);
            mgrs .add(mgr);
            this.applicationToAjaxAppMgrMap.put(application, mgr);
        }
        return mgr;
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        super.valueUnbound(event);
        for (SocketCommunicationManager mgr : mgrs) {
            AtmosphereDontPushHandler.forgetCommunicationMananer(mgr.getId());
        }
    }
}
