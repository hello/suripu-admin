package com.hello.suripu.admin.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WifiInfo {

    public final static String DEFAULT_SSID = "";
    public final static Integer DEFAULT_RSSI = 0;

    public final static Integer RSSI_LOW_CEILING = -90;
    public final static Integer RSSI_MEDIUM_CEILING = -60;
    public final static Integer RSSI_NONE = 0;

    public enum Condition {
        NONE("NONE"),
        BAD("BAD"),
        FAIR("FAIR"),
        GOOD("GOOD");

        private final String value;
        Condition(final String value) {
            this.value = value;
        }
    }

    @JsonProperty("ssid")
    private String ssid;

    @JsonProperty("rssi")
    private Integer rssi;

    @JsonProperty("condition")
    private Condition condition;

    @JsonCreator
    private WifiInfo(final String ssid, final Integer rssi, final Condition condition) {
        this.ssid = ssid;
        this.rssi = rssi;
        this.condition = condition;
    }

    public static WifiInfo createWithRedisResult(final String wifiInfoString) {
        final String[] wifiInfoList = wifiInfoString.split(":");
        if (wifiInfoList.length < 2) {
            return new WifiInfo(DEFAULT_SSID, DEFAULT_RSSI, getCondition(DEFAULT_RSSI));
        }
        return new WifiInfo(wifiInfoList[0].trim(), Integer.valueOf(wifiInfoList[1].trim()), getCondition(Integer.valueOf(wifiInfoList[1].trim())));
    }

    private static Condition getCondition(final Integer rssi) {
        if (rssi == RSSI_NONE) {
            return Condition.NONE;
        }
        else if (rssi <= RSSI_LOW_CEILING) {
            return Condition.BAD;
        }
        else if (rssi <= RSSI_MEDIUM_CEILING) {
            return Condition.FAIR;
        }
        else {
            return Condition.GOOD;
        }
    }
}
