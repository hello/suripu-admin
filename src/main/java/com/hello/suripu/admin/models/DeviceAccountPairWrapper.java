package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import org.joda.time.DateTime;

public class DeviceAccountPairWrapper {

    private final DeviceAccountPair deviceAccountPair;
    private final DeviceKeyStoreRecord deviceKeyStoreRecord;


    public DeviceAccountPairWrapper(DeviceAccountPair deviceAccountPair, DeviceKeyStoreRecord deviceKeyStoreRecord) {
        this.deviceAccountPair = deviceAccountPair;
        this.deviceKeyStoreRecord = deviceKeyStoreRecord;
    }

    @JsonProperty("account_id")
    public Long accountId() {
        return deviceAccountPair.accountId;
    }

    @JsonProperty("sense_id")
    public String senseId() {
        return deviceAccountPair.externalDeviceId;
    }

    @JsonProperty("hw_version")
    public HardwareVersion hardwareVersion() {
        return deviceKeyStoreRecord.hardwareVersion;
    }

    @JsonProperty("paired_on")
    public DateTime pairedOn() {
        return deviceAccountPair.created;
    }
}
