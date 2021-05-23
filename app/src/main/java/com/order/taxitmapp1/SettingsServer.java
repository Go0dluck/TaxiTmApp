package com.order.taxitmapp1;

import android.util.Base64;

class SettingsServer {

    public native String getNativeKeyApiKey();
    public native String getNativeKeyCallKey();

    String getServer() {
        return "https://IP:PORT/common_api/1.0/"; }

    String getApiKey() {
        return new String(Base64.decode(getNativeKeyApiKey(),Base64.DEFAULT));
    }

    String[] getCrewGroupId() {
        return new String[]{"17", "28", "44"}; }

    String[] getParamsOrder(){
        return new String[]{"23", "39", "51"}; // обязательно попорядку
    }

    String getAbortedStateId() {
        return "129"; }

    String getCallKey() {
        return new String(Base64.decode(getNativeKeyCallKey(),Base64.DEFAULT));
    }

    String getCallServer(){
        return "https://IP:PORT/tm_tapi/1.0/";
    }

    String getIp(){
        return "IP:PORT";
    }

    static {
        System.loadLibrary("keys");
    }
}
