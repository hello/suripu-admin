package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;

import java.util.List;

/**
 * Created by zet on 12/10/15.
 */
public class PillAdmin {
    @JsonProperty("device_account_pair")
    private DeviceAccountPair deviceAccountPair;

    @JsonProperty("pill_heart_beats")
    private List<PillHeartBeat> pillHeartBeats;

    public PillAdmin(final DeviceAccountPair deviceAccountPair, final List<PillHeartBeat> pillHeartBeats) {
        this.deviceAccountPair = deviceAccountPair;
        this.pillHeartBeats = pillHeartBeats;
    }
}
