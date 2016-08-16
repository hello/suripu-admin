package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.AggStats;
import org.joda.time.DateTime;

/**
 * Created by jyfan on 7/25/16.
 */
public class AggStatsGenerationRequest {

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("start_date_local_inclusive")
    public final DateTime startDateLocalInclusive;

    @JsonProperty("end_date_local_inclusive")
    public final DateTime endDateLocalInclusive;

    @JsonProperty("overwrite")
    public final Boolean overwrite;

    @JsonCreator
    public AggStatsGenerationRequest(@JsonProperty("account_id") final Long accountId,
                                     @JsonProperty("target_date_local_inclusive") final DateTime startDateLocalInclusive,
                                     @JsonProperty("end_date_local_inclusive") final DateTime endDateLocalInclusive,
                                     @JsonProperty("overwrite") final Boolean overwrite) {
        this.accountId = accountId;
        this.startDateLocalInclusive = startDateLocalInclusive;
        this.endDateLocalInclusive = endDateLocalInclusive;
        this.overwrite = overwrite;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(AggStatsGenerationRequest.class)
                .add("account_id", accountId)
                .add("start_date_local_inclusive", startDateLocalInclusive)
                .add("end_date_local_inclusive", endDateLocalInclusive)
                .add("overwrite", overwrite)
                .toString();
    }

}
