# DontPush OzoneLayer manual #

Once the add-on is set up the add-on should be "seamless" from both developers and end users point of view. Some things to note thought:

  * When making UI changes outside applications active UI thread, ALWAYS synchronize over the application instance. This is common Vaadin stuff and nothing special to OzoneLayer.
  * There are no more requests and responses as with std Vaadin or normal servlet apps. Or there is much less of them. HttpServletRequestListener can still be used to e.g. open and close DB connections, it is called with "fake request" in relevant places.
  * As the connection is maintaned all the time, note that the http session (in which Vaadin app lives in) never dies by default if the user leaves page open.

The setup is slightly more complicated than using normal Vaadin add-on. Three simple steps are needed to get started with OzoneLayer:
  * Add dependencies
  * Update widgetset
  * Configure custom servlet for UIDL communication

## Add dependencies ##

With things are simple, just add this depencency

```
<dependency>
	<groupId>org.vaadin</groupId>
	<artifactId>dontpush-addon-ozonelayer</artifactId>
	<version>1.0.0</version>
</dependency>
```

In case you have build system that don't support transient dependencies, you will need to manually add quite a bunch of jars. If you download the zip file from Vaadin Directory, required jars should be in lib directory.

## Update widgetset ##

The add-on contains some custom logic for the client side to keep up the "continuous connection". If your app already has widgetset recompile it and if you don't create one. This is standar vaadin add-on stuff. See instructions here:

https://vaadin.com/directory/help/using-vaadin-add-ons

If you are updating your widget set manually, this is the dependency you should add to your widget set.
```
    <inherits name="org.vaadin.dontpush.widgetset.DontPushOzoneWidgetset" />
```

## Configure custom servelet for UIDL communication ##

OzoneLayer currently uses two servlets. The other is the modified Vaadin servlet and the second one is AtmosphereServlet that handles all "UIDL" communication.

Add following snippet to your web.xml:

```
        <servlet>
                <description>AtmosphereServlet</description>
                <servlet-name>AtmosphereServlet</servlet-name>
                <servlet-class>org.atmosphere.cpr.AtmosphereServlet</servlet-class>
                <init-param>
                        <!-- prevent deadlocks -->
                        <param-name>org.atmosphere.disableOnStateEvent</param-name>
                        <param-value>true</param-value>
                </init-param>
                <load-on-startup>1</load-on-startup>
                <!--Uncomment if you want to use Servlet 3.0 Async Support -->
                <async-supported>true</async-supported>
        </servlet>
        <servlet-mapping>
                <servlet-name>AtmosphereServlet</servlet-name>
                <url-pattern>/UIDL/*</url-pattern>
        </servlet-mapping>
```

... and change the Vaadin servlet to use org.vaadin.dontpush.server.DontPushOzoneServlet class.
```
	<servlet>
		<servlet-name>Vaadin Application Servlet</servlet-name>
		<servlet-class>org.vaadin.dontpush.server.DontPushOzoneServlet</servlet-class>
...
```

The web.xml file in the demo project can behave as a reference:

[web.xml](http://code.google.com/p/dontpush/source/browse/trunk/dontpush-root/demo-ozonelayer/src/main/webapp/WEB-INF/web.xml)

The AtmosphereServlet also needs its own "atmosphere.xml" configuration file. Copy the one from demo to your own projects webapp(WebContent/whatever)/META-INF/ directory.

http://code.google.com/p/dontpush/source/browse/trunk/dontpush-root/demo-ozonelayer/src/main/webapp/META-INF/atmosphere.xml