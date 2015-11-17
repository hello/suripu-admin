package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.models.SleepFeedback;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/v1/feedback")
public class FeedbackResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackResources.class);

    private final FeedbackDAO feedbackDAO;

    public FeedbackResources(final FeedbackDAO feedbackDAO) {
        this.feedbackDAO = feedbackDAO;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Path("/{account_id}/{night}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TimelineFeedback> retrieveFeedback(@Auth final AccessToken token,
                                                   @PathParam("account_id") final Long accountId,
                                                   @PathParam("night") final String night){

        if (accountId == null || night == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing path params /account_id/night").build());
        }

        final DateTime dateOfNight = DateTime.parse(night);
        final DateTime dateOfNightUTC = new DateTime(dateOfNight.getMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        return feedbackDAO.getForNight(accountId, dateOfNightUTC);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @Timed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertSleepFeedback(@Auth final AccessToken token,
                                        @Valid final SleepFeedback sleepFeedback){

        feedbackDAO.insert(sleepFeedback);
        return Response.noContent().build();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @Timed
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upsertTimelineFeedback(@Auth final AccessToken token,
                                           @Valid final TimelineFeedback timelineFeedback){

        if (!timelineFeedback.accountId.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing account ID").build());
        }

        feedbackDAO.insertTimelineFeedback(timelineFeedback.accountId.get(), timelineFeedback);
        return Response.noContent().build();
    }
}
