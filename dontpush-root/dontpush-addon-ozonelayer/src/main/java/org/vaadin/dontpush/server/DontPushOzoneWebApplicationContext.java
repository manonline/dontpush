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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.terminal.gwt.server.DontPushWebBrowser;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

/**
 * Web application context for Vaadin applications.
 * 
 * This is automatically added as a
 * {@link javax.servlet.http.HttpSessionBindingListener} when added to a
 * {@link javax.servlet.http.HttpSession}.
 * 
 * @author Mark Thomas
 */
@SuppressWarnings("serial")
public class DontPushOzoneWebApplicationContext extends WebApplicationContext {

    private static final HttpServletResponse FAKE_RESPONSE;
    private static final HttpServletRequest FAKE_REQUEST;
    static {
        InvocationHandler dummyHandler = new InvocationHandler() {
            public Object invoke(Object arg0, Method arg1, Object[] arg2)
                    throws Throwable {
                return null;
            }
        };
        FAKE_REQUEST = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class[] { HttpServletRequest.class }, dummyHandler);
        FAKE_RESPONSE = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class[] { HttpServletResponse.class }, dummyHandler);
    }

    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    private final Class<SocketCommunicationManager> communicationManagerClass;
    private Collection<SocketCommunicationManager> mgrs = new LinkedList<SocketCommunicationManager>();

    public DontPushOzoneWebApplicationContext(HttpSession session,
            Class<SocketCommunicationManager> communicationManagerClass) {
        super();
        this.session = session;
        this.communicationManagerClass = communicationManagerClass;
        browser = new DontPushWebBrowser();
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
            AtmosphereDontPushHandler.setCommunicationManager(mgr.getId(), mgr);
            mgrs.add(mgr);
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

    public void trxStart(Application application, Object request) {
        super.startTransaction(application, request);
        if (application instanceof HttpServletRequestListener) {
            ((HttpServletRequestListener) application).onRequestStart(
                    FAKE_REQUEST, FAKE_RESPONSE);
        }
    }

    public void trxEnd(Application application, Object request) {
        if (application instanceof HttpServletRequestListener) {
            ((HttpServletRequestListener) application).onRequestEnd(
                    FAKE_REQUEST, FAKE_RESPONSE);
        }
        super.endTransaction(application, request);
    }

    @Override
    public DontPushWebBrowser getBrowser() {
        return (DontPushWebBrowser) super.getBrowser();
    }
}
