package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.db.UptimeDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.diagnostic.Count;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.tracking.Category;
import com.hello.suripu.core.tracking.TrackingDAO;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Hours;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/v1/diagnostic")
public class DiagnosticResources {

    private final AccountDAO accountDAO;
    private final DeviceDAO deviceDAO;
    private final TrackingDAO trackingDAO;
    private final UptimeDAO uptimeDAO;

    private final static Ordering<Count> byMillisOrdering = new Ordering<Count>() {
        public int compare(Count left, Count right) {
            return Longs.compare(left.date.getMillis(), right.date.getMillis());
        }
    };

    public DiagnosticResources(final AccountDAO accountDAO,
                               final DeviceDAO deviceDAO,
                               final TrackingDAO trackingDAO,
                               final UptimeDAO uptimeDAO) {
        this.accountDAO = accountDAO;
        this.deviceDAO = deviceDAO;
        this.trackingDAO = trackingDAO;
        this.uptimeDAO = uptimeDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Path("/uptime/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Count> uptime(@Auth final AccessToken accessToken,
                              @PathParam("email") final String email,
                              @DefaultValue("true") @QueryParam("padded") Boolean padded) {

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if(!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(
                    new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "Account not found")).build());
        }

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountIdOptional.get());
        if(!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(
                    new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "Device not found")).build());
        }

        final DateTime start = DateTime.now(DateTimeZone.UTC).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        final Hours hours = Hours.hoursBetween(start, DateTime.now(DateTimeZone.UTC).minusDays(10));
        final Map<Long, Count> pad = Maps.newHashMap();
        for(int i =1; i < hours.getHours(); i++) {
            final DateTime dt = start.minusHours(i);
            pad.put(dt.getMillis(), new Count(dt, 0));
        }

        final List<Count> counts = uptimeDAO.uptime(accountIdOptional.get());
        for(final Count c : counts) {
            pad.put(c.date.getMillis(), c);
        }

        return byMillisOrdering.sortedCopy(pad.values());

    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @Timed
    @PUT
    @Path("/track/uptime/{email}")
    public void Track(@Auth final AccessToken accessToken,
                      @PathParam("email") final String email) {

        final Optional<Account> accountOptional = accountDAO.getByEmail(email);
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<DeviceAccountPair> pairs = deviceDAO.getSensesForAccountId(accountOptional.get().id.get());
        for(final DeviceAccountPair pair : pairs) {
            trackingDAO.insert(pair.externalDeviceId, pair.internalDeviceId, pair.accountId, Category.UPTIME.value);
        }
    }
}
