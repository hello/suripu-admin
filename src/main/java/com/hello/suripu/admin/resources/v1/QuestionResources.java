package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import io.dropwizard.auth.Auth;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Created by ksg on 01/29/16
 */

@Path("/v1/questions")

public class QuestionResources {

    private final QuestionResponseDAO questionResponseDAO;
    private final TimeZoneHistoryDAODynamoDB tzHistoryDAO;

    private final Map<String, Integer> triggerQuestions = Maps.newHashMap();

    public QuestionResources(final QuestionResponseDAO questionResponseDAO,
                             final TimeZoneHistoryDAODynamoDB tzHistoryDAO) {
        this.questionResponseDAO = questionResponseDAO;
        this.tzHistoryDAO = tzHistoryDAO;

        // populate questions -- not the best way.
        final List<Question> questions = this.questionResponseDAO.getAllQuestions();
        for (final Question question : questions) {
            if (question.text.contains("unusual bright light")) {
                triggerQuestions.put("light", question.id);
            }
        }
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Path("/anomaly_question")
    public void anomaly_question(@Auth final AccessToken accessToken,
                                 @QueryParam("sensor") final String sensorName,
                                 @QueryParam("account_id") final Long accountId) {

        if (sensorName.equals("light")) {
            final boolean result = this.insertLightAnomalyQuestion(accountId);
            if (!result) {
                throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            }
        } else {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("invalid sensor name").build());

        }
    }

    private boolean insertLightAnomalyQuestion(final Long accountId) {
        final DateTime today = getToday(accountId);
        final Long inserted = this.questionResponseDAO.insertAccountQuestion(accountId, triggerQuestions.get("light"), today, today.plusDays(1));
        return inserted > 0;
    }

    private DateTime getToday(final Long accountId) {
        final Optional<TimeZoneHistory> tzHistory = this.tzHistoryDAO.getCurrentTimeZone(accountId);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        if (tzHistory.isPresent()) {
            return now.plusMillis(tzHistory.get().offsetMillis).withTimeAtStartOfDay();
        }
        return now.plusMillis(TimeZoneHistory.FALLBACK_OFFSET_MILLIS).withTimeAtStartOfDay();
    }
}
