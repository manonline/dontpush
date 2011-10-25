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

import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

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
public class DontPushOzoneServlet extends ApplicationServlet {

    @Override
    protected WebApplicationContext getApplicationContext(HttpSession session) {
        WebApplicationContext cx = (WebApplicationContext)session.getAttribute(WebApplicationContext.class.getName());
        if (cx == null) {
            cx = new DontPushOzoneWebApplicationContext(session);
            session.setAttribute(WebApplicationContext.class.getName(), cx);
        }
        return cx;
    }
}
