package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 2/2/16.
 */
public class AnomalyQuestion {

    final public Long accountId;
    final public String sensor;
    final public String nightDate;

    @JsonCreator
    public AnomalyQuestion(@JsonProperty("account_id") final Long accountId,
                           @JsonProperty("sensor") final String sensor,
                           @JsonProperty("night_date") final String nightDate) {
        this.accountId = accountId;
        this.sensor = sensor;
        this.nightDate = nightDate;
    }
}
