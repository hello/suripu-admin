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

    @JsonProperty("publication_date_local")
    public final Optional<String> publicationDateLocalStringOptional;

    public final DateTime publicationDateLocal;

    @JsonCreator
    public InsightsGenerationRequest(@JsonProperty("account_id") final Long accountId,
                                     @JsonProperty("category_string") final String categoryString,
                                     @JsonProperty("publication_date_local") final Optional<String> publicationDateLocalStringOptional) {
        this.accountId = accountId;
        this.categoryString = categoryString;
        this.insightCategory = InsightCard.Category.fromString(categoryString);
        this.publicationDateLocalStringOptional = publicationDateLocalStringOptional;
        if (publicationDateLocalStringOptional.isPresent()) {
            this.publicationDateLocal = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(publicationDateLocalStringOptional.get());
        } else {
            this.publicationDateLocal = DateTime.now(DateTimeZone.UTC);
        }
    }

}
