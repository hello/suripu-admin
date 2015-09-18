package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Insights.InsightCard;

/**
 * Created by jyfan on 9/17/15.
 */
public class InsightsGenerationRequest {

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("category_string")
    public final String categoryString;

    @JsonProperty("insight_category")
    public final InsightCard.Category insightCategory;

    @JsonCreator
    public InsightsGenerationRequest(@JsonProperty("account_id") final Long accountId,
                                     @JsonProperty("category_string") final String categoryString) {
        this.accountId = accountId;
        this.categoryString = categoryString;
        this.insightCategory = InsightCard.Category.fromString(categoryString);
    }

}
