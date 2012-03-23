package org.vaadin.dontpush.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.Application;
import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

/**
 * This class can be used to run any kind of logic in an application window
 * scoped transaction. It does not matter which thread is used to call this
 * class. By starting a web transaction, the required information is adapted to
 * the thread and so the thread seems to be a request thread.<br/>
 * This class offeres two methods to be called:<br/>
 * <li><i>execute</i> - which starts a web transaction, but does not synchronize
 * by the application instance. So the UI will not become frozen. Can be used
 * for long running operations that require a properly configured thread, but do
 * not change any UI state.</li> <li><i>executeSafe</i> - the same as execute,
 * but synchronizes the call by the application instance. Should be used for
 * short running UI operations.</li>
 * 
 */
public abstract class UnitOfWork {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Application application;
    private final Window window;

    public UnitOfWork(Application application, Window window) {
        this.application = application;
        this.window = window;
    }

    /**
     * @param component
     *            the component in which "context" the task will be run.
     */
    public UnitOfWork(Component component) {
        Window w = component.getWindow();
        if (w.getParent() != null) {
            w = w.getParent();
        }
        application = w.getApplication();
        window = w;
    }

    /**
     * Starts a web transaction, but does not synchronize by the application
     * instance. So the UI will not become frozen. Can be used for long running
     * operations that require a properly configured thread, but do not change
     * any UI state.
     */
    public void execute() {

    }

    /**
     * The same as execute, but synchronizes the call by the application
     * instance. Should be used for short running UI operations.
     */
    public void executeSafe() {
        synchronized (application) {
            internalExecute();
        }
    }

    /**
     * Internal execute.
     */
    private void internalExecute() {
        if (window.getParent() != null) {
            logger.warn("Syncing only allowed for top level windows!");
            return;
        }

        DontPushOzoneWebApplicationContext context = ((DontPushOzoneWebApplicationContext) application
                .getContext());
        SocketCommunicationManager cm = (SocketCommunicationManager) context
                .getApplicationManager(application, null);
        VaadinWebSocket socket = cm.getSocketForWindow(window);
        try {
            context.trxStart(application, socket);

            run();

        } finally {
            context.trxEnd(application, socket);
        }
    }

    /**
     * Subclasses have to implement that method. For detailed information about
     * the way to invoke this class, see {@link UnitOfWork}.
     */
    protected abstract void run();

    /**
     * Runs given Runnable so that the application gets properly synchronized
     * and all "transaction listeners" gets properly called before and after the
     * given task. Note that this method may block if the another thread is
     * currently accessing the same application.
     * 
     * @param r
     *            the task that should be run.
     * @param c
     *            a component as a reference in which "context" the task should
     *            be run
     */
    public static void invoke(final Runnable r, Component c) {
        new UnitOfWork(c) {
            @Override
            protected void run() {
                r.run();
            }
        }.executeSafe();
    }

}
