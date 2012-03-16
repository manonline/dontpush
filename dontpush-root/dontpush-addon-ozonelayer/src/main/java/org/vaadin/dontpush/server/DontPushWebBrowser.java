package org.vaadin.dontpush.server;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.vaadin.terminal.gwt.client.VBrowserDetails;
import com.vaadin.terminal.gwt.server.WebBrowser;

/**
 * A temporary workaround since WebBrowser#updateClientSideDetails methods are
 * package protected. All the state if the superclass is hold in the
 * DontPushWebBrowser.class. The super class is only required for polymorphic
 * compatibility.
 */
public class DontPushWebBrowser extends WebBrowser {
    private int screenHeight = 0;
    private int screenWidth = 0;
    private String browserApplication = null;
    private Locale locale;
    private String address;
    private boolean secureConnection;
    private int timezoneOffset = 0;
    private int rawTimezoneOffset = 0;
    private int dstSavings;
    private boolean dstInEffect;
    private boolean touchDevice;

    private VBrowserDetails browserDetails;
    private long clientServerTimeDelta;

    /**
     * There is no default-theme for this terminal type.
     * 
     * @return Always returns null.
     */
    public String getDefaultTheme() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Terminal#getScreenHeight()
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Terminal#getScreenWidth()
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * Get the browser user-agent string.
     * 
     * @return The raw browser userAgent string
     */
    public String getBrowserApplication() {
        return browserApplication;
    }

    /**
     * Gets the IP-address of the web browser. If the application is running
     * inside a portlet, this method will return null.
     * 
     * @return IP-address in 1.12.123.123 -format
     */
    public String getAddress() {
        return address;
    }

    /** Get the default locate of the browser. */
    public Locale getLocale() {
        return locale;
    }

    /** Is the connection made using HTTPS? */
    public boolean isSecureConnection() {
        return secureConnection;
    }

    /**
     * Tests whether the user is using Firefox.
     * 
     * @return true if the user is using Firefox, false if the user is not using
     *         Firefox or if no information on the browser is present
     */
    public boolean isFirefox() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isFirefox();
    }

    /**
     * Tests whether the user is using Internet Explorer.
     * 
     * @return true if the user is using Internet Explorer, false if the user is
     *         not using Internet Explorer or if no information on the browser
     *         is present
     */
    public boolean isIE() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isIE();
    }

    /**
     * Tests whether the user is using Safari.
     * 
     * @return true if the user is using Safari, false if the user is not using
     *         Safari or if no information on the browser is present
     */
    public boolean isSafari() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isSafari();
    }

    /**
     * Tests whether the user is using Opera.
     * 
     * @return true if the user is using Opera, false if the user is not using
     *         Opera or if no information on the browser is present
     */
    public boolean isOpera() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isOpera();
    }

    /**
     * Tests whether the user is using Chrome.
     * 
     * @return true if the user is using Chrome, false if the user is not using
     *         Chrome or if no information on the browser is present
     */
    public boolean isChrome() {
        if (browserDetails == null) {
            return false;
        }

        return browserDetails.isChrome();
    }

    /**
     * Gets the major version of the browser the user is using.
     * 
     * <p>
     * Note that Internet Explorer in IE7 compatibility mode might return 8 in
     * some cases even though it should return 7.
     * </p>
     * 
     * @return The major version of the browser or -1 if not known.
     */
    public int getBrowserMajorVersion() {
        if (browserDetails == null) {
            return -1;
        }

        return browserDetails.getBrowserMajorVersion();
    }

    /**
     * Gets the minor version of the browser the user is using.
     * 
     * @see #getBrowserMajorVersion()
     * 
     * @return The minor version of the browser or -1 if not known.
     */
    public int getBrowserMinorVersion() {
        if (browserDetails == null) {
            return -1;
        }

        return browserDetails.getBrowserMinorVersion();
    }

    /**
     * Tests whether the user is using Linux.
     * 
     * @return true if the user is using Linux, false if the user is not using
     *         Linux or if no information on the browser is present
     */
    public boolean isLinux() {
        return browserDetails.isLinux();
    }

    /**
     * Tests whether the user is using Mac OS X.
     * 
     * @return true if the user is using Mac OS X, false if the user is not
     *         using Mac OS X or if no information on the browser is present
     */
    public boolean isMacOSX() {
        return browserDetails.isMacOSX();
    }

    /**
     * Tests whether the user is using Windows.
     * 
     * @return true if the user is using Windows, false if the user is not using
     *         Windows or if no information on the browser is present
     */
    public boolean isWindows() {
        return browserDetails.isWindows();
    }

    /**
     * Returns the browser-reported TimeZone offset in milliseconds from GMT.
     * This includes possible daylight saving adjustments, to figure out which
     * TimeZone the user actually might be in, see
     * {@link #getRawTimezoneOffset()}.
     * 
     * @see WebBrowser#getRawTimezoneOffset()
     * @return timezone offset in milliseconds, 0 if not available
     */
    public Integer getTimezoneOffset() {
        return timezoneOffset;
    }

    /**
     * Returns the browser-reported TimeZone offset in milliseconds from GMT
     * ignoring possible daylight saving adjustments that may be in effect in
     * the browser.
     * <p>
     * You can use this to figure out which TimeZones the user could actually be
     * in by calling {@link TimeZone#getAvailableIDs(int)}.
     * </p>
     * <p>
     * If {@link #getRawTimezoneOffset()} and {@link #getTimezoneOffset()}
     * returns the same value, the browser is either in a zone that does not
     * currently have daylight saving time, or in a zone that never has daylight
     * saving time.
     * </p>
     * 
     * @return timezone offset in milliseconds excluding DST, 0 if not available
     */
    public Integer getRawTimezoneOffset() {
        return rawTimezoneOffset;
    }

    /**
     * Gets the difference in minutes between the browser's GMT TimeZone and
     * DST.
     * 
     * @return the amount of minutes that the TimeZone shifts when DST is in
     *         effect
     */
    public int getDSTSavings() {
        return dstSavings;
    }

    /**
     * Determines whether daylight savings time (DST) is currently in effect in
     * the region of the browser or not.
     * 
     * @return true if the browser resides at a location that currently is in
     *         DST
     */
    public boolean isDSTInEffect() {
        return dstInEffect;
    }

    /**
     * Returns the current date and time of the browser. This will not be
     * entirely accurate due to varying network latencies, but should provide a
     * close-enough value for most cases. Also note that the returned Date
     * object uses servers default time zone, not the clients.
     * 
     * @return the current date and time of the browser.
     * @see #isDSTInEffect()
     * @see #getDSTSavings()
     * @see #getTimezoneOffset()
     */
    public Date getCurrentDate() {
        return new Date(new Date().getTime() + clientServerTimeDelta);
    }

    /**
     * @return true if the browser is detected to support touch events
     */
    public boolean isTouchDevice() {
        return touchDevice;
    }

    /**
     * For internal use by AbstractApplicationServlet/AbstractApplicationPortlet
     * only. Updates all properties in the class according to the given
     * information.
     * 
     * @param sw
     *            Screen width
     * @param sh
     *            Screen height
     * @param tzo
     *            TimeZone offset in minutes from GMT
     * @param rtzo
     *            raw TimeZone offset in minutes from GMT (w/o DST adjustment)
     * @param dstSavings
     *            the difference between the raw TimeZone and DST in minutes
     * @param dstInEffect
     *            is DST currently active in the region or not?
     * @param curDate
     *            the current date in milliseconds since the epoch
     * @param touchDevice
     */
    public void updateClientSideDetails(String sw, String sh, String tzo,
            String rtzo, String dstSavings, String dstInEffect, String curDate,
            boolean touchDevice) {
        if (sw != null) {
            try {
                screenHeight = Integer.parseInt(sh);
                screenWidth = Integer.parseInt(sw);
            } catch (final NumberFormatException e) {
                screenHeight = screenWidth = 0;
            }
        }
        if (tzo != null) {
            try {
                // browser->java conversion: min->ms, reverse sign
                timezoneOffset = -Integer.parseInt(tzo) * 60 * 1000;
            } catch (final NumberFormatException e) {
                timezoneOffset = 0; // default gmt+0
            }
        }
        if (rtzo != null) {
            try {
                // browser->java conversion: min->ms, reverse sign
                rawTimezoneOffset = -Integer.parseInt(rtzo) * 60 * 1000;
            } catch (final NumberFormatException e) {
                rawTimezoneOffset = 0; // default gmt+0
            }
        }
        if (dstSavings != null) {
            try {
                // browser->java conversion: min->ms
                this.dstSavings = Integer.parseInt(dstSavings) * 60 * 1000;
            } catch (final NumberFormatException e) {
                this.dstSavings = 0; // default no savings
            }
        }
        if (dstInEffect != null) {
            this.dstInEffect = Boolean.parseBoolean(dstInEffect);
        }
        if (curDate != null) {
            try {
                long curTime = Long.parseLong(curDate);
                clientServerTimeDelta = curTime - new Date().getTime();
            } catch (final NumberFormatException e) {
                clientServerTimeDelta = 0;
            }
        }
        this.touchDevice = touchDevice;

    }

    /**
     * For internal use by AbstractApplicationServlet/AbstractApplicationPortlet
     * only. Updates all properties in the class according to the given
     * information.
     * 
     * @param locale
     *            The browser primary locale
     * @param address
     *            The browser ip address
     * @param secureConnection
     *            true if using an https connection
     * @param agent
     *            Raw userAgent string from the browser
     */
    public void updateRequestDetails(Locale locale, String address,
            boolean secureConnection, String agent) {
        this.locale = locale;
        this.address = address;
        this.secureConnection = secureConnection;
        if (agent != null) {
            browserApplication = agent;
            browserDetails = new VBrowserDetails(agent);
        }

    }

    public void updateClientSideDetails(String params) {
        String[] split = params.split("&");
        String sw = null;
        String dstInEffect = null;
        String dstSavings = null;
        String rtzo = null;
        String tzo = null;
        String sh = null;
        boolean touchDevice = false;
        String curDate = null;

        for (String string : split) {
            String[] split2 = string.split("=");
            if (split2.length == 2) {
                String key = split2[0];
                String value = split2[1];
                if (key.equals("sw")) {
                    sw = value;
                } else if (key.equals("sh")) {
                    sh = value;
                } else if (key.equals("tzo")) {
                    tzo = value;
                } else if (key.equals("rtzo")) {
                    rtzo = value;
                } else if (key.equals("dstd")) {
                    dstSavings = value;
                } else if (key.equals("dston")) {
                    dstInEffect = value;
                } else if (key.equals("curdate")) {
                    curDate = value;
                } else if (key.equals("td")) {
                    touchDevice = true;
                }
            }
        }
        updateClientSideDetails(sw, sh, tzo, rtzo, dstSavings, dstInEffect,
                curDate, touchDevice);
    }

}
