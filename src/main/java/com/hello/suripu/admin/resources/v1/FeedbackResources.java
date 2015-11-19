package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.admin.Util;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
    private final AccountDAO accountDAO;

    public FeedbackResources(final FeedbackDAO feedbackDAO, final AccountDAO accountDAO) {
        this.feedbackDAO = feedbackDAO;
        this.accountDAO = accountDAO;
    }


    @ScopesAllowed(OAuthScope.ADMINISTRATION_READ)
    @Timed
    @GET
    @Path("/{email}/{night}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TimelineFeedback> retrieveFeedback(@Auth final AccessToken token,
                                                   @PathParam("email") final String email,
                                                   @PathParam("night") final String night){

        if (email == null || night == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing path params /email/night").build());
        }

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(404).entity(new JsonError(404, "Account not found!")).build());
        }

        final DateTime dateOfNight = DateTime.parse(night);
        final DateTime dateOfNightUTC = new DateTime(dateOfNight.getMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        return feedbackDAO.getForNight(accountIdOptional.get(), dateOfNightUTC);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @Timed
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response undoFeedbackUpdate(@Auth final AccessToken token,
                                       @PathParam("id") final Long id) {

        final int updated = feedbackDAO.undoFeedbackUpdate(id);

        if (updated == 0) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(new JsonError(404, "Feedback not found!")).build());
        }

        return Response.noContent().build();
    }

}
