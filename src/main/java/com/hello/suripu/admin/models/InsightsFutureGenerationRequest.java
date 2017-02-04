package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by jyfan on 2/3/17.
 */
public class InsightsFutureGenerationRequest {

        @JsonProperty("account_id")
        public final Long accountId;

        @JsonProperty("category_string")
        public final String categoryString;

        public final InsightCard.Category insightCategory;

        @JsonProperty("date_visible_local_string")
        public final String dateVisibleLocalString;

        public final DateTime dateVisibleLocal;

        @JsonCreator
        public InsightsFutureGenerationRequest(@JsonProperty("account_id") final Long accountId,
                                         @JsonProperty("category_string") final String categoryString,
                                         @JsonProperty("date_visible_local_string") final String dateVisibleLocalString) {
            this.accountId = accountId;
            this.categoryString = categoryString;
            this.insightCategory = InsightCard.Category.fromString(categoryString);
            this.dateVisibleLocalString = dateVisibleLocalString;
            this.dateVisibleLocal = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dateVisibleLocalString);
        }

}
