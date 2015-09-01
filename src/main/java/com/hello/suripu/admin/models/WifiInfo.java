package com.hello.suripu.admin.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WifiInfo {

    @JsonProperty("ssid")
    private String ssid;

    @JsonProperty("rssi")
    private Integer rssi;

    @JsonCreator
    private WifiInfo(final String ssid, final Integer rssi) {
        this.ssid = ssid;
        this.rssi = rssi;
    }

    public static WifiInfo createWithRedisResult(final String wifiInfoString) {
        final String[] wifiInfoList = wifiInfoString.split(":");
        if (wifiInfoList.length < 2) {
            return new WifiInfo("", 0);
        }
        return new WifiInfo(wifiInfoList[0].trim(), Integer.valueOf(wifiInfoList[1].trim()));
    }
}
