package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by jyfan on 9/17/15.
 */
public class InsightsGenerationRequest {

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("category_string")
    public final String categoryString;

    public final InsightCard.Category insightCategory;

    @JsonProperty("date_visible_local_string_optional")
    public final Optional<String> dateVisibleLocalStringOptional;

    public final DateTime dateVisibleLocal;

    @JsonCreator
    public InsightsGenerationRequest(@JsonProperty("account_id") final Long accountId,
                                     @JsonProperty("category_string") final String categoryString,
                                     @JsonProperty("date_visible_local_string_optional") final Optional<String> dateVisibleLocalStringOptional) {
        this.accountId = accountId;
        this.categoryString = categoryString;
        this.insightCategory = InsightCard.Category.fromString(categoryString);
        this.dateVisibleLocalStringOptional = dateVisibleLocalStringOptional;
        if (dateVisibleLocalStringOptional.isPresent()) {
            this.dateVisibleLocal = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dateVisibleLocalStringOptional.get());
        } else {
            this.dateVisibleLocal = DateTime.now(DateTimeZone.UTC);
        }
    }

}
