# Developing DontPush OzoneLayer #

To develop DontPush OzoneLayer check out these two maven project:

http://dontpush.googlecode.com/svn/trunk/dontpush-root/dontpush-addon-ozonelayer/
http://dontpush.googlecode.com/svn/trunk/dontpush-root/demo-ozonelayer/

The former builds the actual add-on jar and the latter is a war project that can be used to test it. The demo project also contains a simple WebDriver based integration test against the demo app.

First build the add-on project with "mvn install". Then do the same for the demo. The latter will take some time as it will trigger GWT compilation. At the end of the install process maven build lauches the application to a jetty server and runs some tests against it.

During normal development process you'll have the demo project running in jetty with "mvn jetty:run" command. The app can also be deployed to an other server. E.g. to Tomcat with Eclipse WTP platform is fine if you have WTP m2e plugin installed.

If you have both projects in IDE the IDE can inject most changes on the fly to JVM. If changes cannnot be deployd, re-install the add-on project and relauch the demo app.

Client side changes needs GWT compilation (unless you are using GWT development mode). It can be forced with "mvn clean gwt:compile".

Feel free to add new test cases to the demo project.

Send patches via the google code page