package com.example.taxitmapp;

class SettingsServer {

    String getServer() {
        return "https://194.176.118.233:8089/common_api/1.0/"; }

    String getApiKey() {
        return "23oli;hfghdfaslikvghfkljsvghghf";
    }

    String[] getCrewGroupId() {
        return new String[]{"17", "28", "44"}; }

    String[] getParamsOrder(){
        return new String[]{"23", "39", "51"}; // обязательно попорядку
    }

    String getAbortedStateId() {
        return "129"; }

    String getCallKey() {
        return "23fdhxdtfshjfjhfjhkjj,lhkg,l"; }

    String getCallServer(){
        return "https://194.176.118.233:8089/tm_tapi/1.0/";
    }
}
