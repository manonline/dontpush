/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vaadin.dontpush.server;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

import javax.servlet.http.HttpSession;

/**
 * Web application context for Vaadin applications.
 *
 * This is automatically added as a {@link javax.servlet.http.HttpSessionBindingListener} when
 * added to a {@link javax.servlet.http.HttpSession}.
 *
 * @author Mark Thomas
 * @version 1.0.0
 * @since 1.0.0
 */
public class DontPushOzoneWebApplicationContext extends WebApplicationContext {

    public DontPushOzoneWebApplicationContext(HttpSession session) {
        super();
        this.session = session;
    }

    @Override
    public CommunicationManager getApplicationManager(Application application, AbstractApplicationServlet servlet) {
        CommunicationManager mgr = (CommunicationManager)this.applicationToAjaxAppMgrMap.get(application);

        if (mgr == null) {
            // Creates new manager
            mgr = new SocketCommunicationManager(application);
            this.applicationToAjaxAppMgrMap.put(application, mgr);
        }
        return mgr;
    }
}
