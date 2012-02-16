package com.vaadin.terminal.gwt.server;

import com.vaadin.terminal.gwt.server.WebBrowser;

public class DontPushWebBrowser extends WebBrowser {

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
            if(split2.length == 2) {
                String key = split2[0];
                String value = split2[1];
                if(key.equals("sw")) {
                    sw = value;
                } else if (key.equals("sh")) {
                    sh = value;
                } else if (key.equals("tzo")){
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
        super.updateClientSideDetails(sw, sh, tzo, rtzo, dstSavings, dstInEffect,
                curDate, touchDevice);
    }

}
