package com.example.taxitmapp;

public class SettingsServer {
    private String server = "https://194.176.118.233:8089/common_api/1.0/";
    private String apiKey = "23oli;hfghdfaslikvghfkljsvghghf";
    private String crewGroupId = "17";
    private String abortedStateId = "129";

    public String getServer() { return server; }

    public String getApiKey() {
        return apiKey;
    }

    public String getCrewGroupId() { return crewGroupId; }

    public String getAbortedStateId() { return abortedStateId; }
}
