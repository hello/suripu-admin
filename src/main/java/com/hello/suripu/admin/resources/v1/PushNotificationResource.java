package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.notifications.HelloPushMessage;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class PushNotificationResource {

    private final AccountDAO accountDAO;
    private final NotificationSubscriptionsDAO notificationSubscriptionsDAO;

    public PushNotificationResource(final AccountDAO accountDAO,
                                    final NotificationSubscriptionsDAO notificationSubscriptionsDAO) {
        this.accountDAO = accountDAO;
        this.notificationSubscriptionsDAO = notificationSubscriptionsDAO;
    }

    @ScopesAllowed(OAuthScope.ADMINISTRATION_WRITE)
    @POST
    @Path("{email}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response send(@Auth AccessToken accessToken, @PathParam("email") String email, Map<String, String> data) {

        final Optional<Account> accountOptional = accountDAO.getByEmail(email);
        if(!accountOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final List<MobilePushRegistration> registrations = notificationSubscriptionsDAO.getSubscriptions(accountOptional.get().id.or(0L));
        for(final MobilePushRegistration registration : registrations) {
            final HelloPushMessage message = new HelloPushMessage(
                    data.getOrDefault("body", "Missing body"),
                    data.getOrDefault("target", "missing target"),
                    data.getOrDefault("details", "missing details")
            );
        }

        return Response.ok().build();
    }
}
