package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.notifications.HelloPushMessage;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class NotificationResources {


    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationResources.class);

    private final AccountDAO accountDAO;
    private final MobilePushNotificationProcessor pushNotificationProcessor;

    public NotificationResources(final AccountDAO accountDAO, MobilePushNotificationProcessor pushNotificationProcessor) {
        this.accountDAO = accountDAO;
        this.pushNotificationProcessor = pushNotificationProcessor;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @POST
    @Path("/send/{email}")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public Response send(
            @Auth final AccessToken accessToken,
            @PathParam("email") final String email,
            @Valid final HelloPushMessage message) {

        LOGGER.debug("email: {}", email);
        LOGGER.debug("push target: {}", message.target);
        LOGGER.debug("push details: {}", message.details);
        LOGGER.debug("push body: {}", message.body);

        final Optional<Account> accountOptional = accountDAO.getByEmail(email);
        if(!accountOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        pushNotificationProcessor.push(accountOptional.get().id.get(), message);
        return Response.ok().build();
    }
}
