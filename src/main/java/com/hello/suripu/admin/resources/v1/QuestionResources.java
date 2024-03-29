package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;

import com.codahale.metrics.annotation.Timed;
import com.hello.suripu.admin.models.AnomalyQuestion;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.QuestionProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by ksg on 01/29/16
 */

@Path("/v1/questions")
public class QuestionResources {

    private static final int TOO_OLD_THRESHOLD = 2; // night date older than this number of days

    private final AccountReadDAO accountReadDAO;
    private final QuestionProcessor questionProcessor;
    private final TimeZoneHistoryDAODynamoDB tzHistoryDAO;

    public QuestionResources(final AccountReadDAO accountReadDAO,
                             final QuestionProcessor questionProcessor,
                             final TimeZoneHistoryDAODynamoDB tzHistoryDAO) {
        this.accountReadDAO = accountReadDAO;
        this.questionProcessor = questionProcessor;
        this.tzHistoryDAO = tzHistoryDAO;
    }

    @ScopesAllowed({OAuthScope.ADMIN_QUESTIONS_WRITE})
    @Timed
    @POST
    @Path("/anomaly")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertAnomalyQuestion(@Auth final AccessToken accessToken,
                                      final AnomalyQuestion anomalyQuestion) {

        final Optional<Account> optionalAccount = this.accountReadDAO.getById(anomalyQuestion.accountId);
        if (!optionalAccount.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("account_id does not exist").build());
        }

        if (anomalyQuestion.sensor.equals("light")) {
            final DateTime nightDate = DateTimeUtil.ymdStringToDateTime(anomalyQuestion.nightDate);
            final DateTime today = getToday(anomalyQuestion.accountId);
            final boolean result = this.questionProcessor.insertLightAnomalyQuestion(anomalyQuestion.accountId, nightDate, today);
            if (!result) {
                return Response.noContent().build();
            }
        } else {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("invalid sensor name").build());
        }
        return Response.ok().entity("question inserted").build();
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
