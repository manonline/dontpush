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

package org.vaadin.dontpush.vwebsocket.client;

import com.google.gwt.core.client.JavaScriptObject;

public final class WebSocket extends JavaScriptObject {
    protected WebSocket() {
    }

    /**
     * TODO proper error handling
     * 
     * @param uri
     * @return
     */
    public static native WebSocket bind(String uri)
    /*-{
     	return new $wnd.WebSocket(uri);
     }-*/;

    public final native void setListener(WebSocketListener listener) 
    /*-{
    	this.onopen = function() {
             listener.@org.vaadin.dontpush.vwebsocket.client.WebSocketListener::connected()();
        };
    	this.onmessage = function(response) {
             listener.@org.vaadin.dontpush.vwebsocket.client.WebSocketListener::message(Ljava/lang/String;)( response.data );
        };
    	this.onclose = function() {
             listener.@org.vaadin.dontpush.vwebsocket.client.WebSocketListener::disconnected()();
        };
    }-*/;

    public final native void send(String message) 
    /*-{
        this.send(message);
	}-*/;

}