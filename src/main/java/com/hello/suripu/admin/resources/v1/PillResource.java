package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.db.DeviceAdminDAO;
import com.hello.suripu.admin.models.PillAdmin;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/v1/pill")
public class PillResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PillResource.class);

    private final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;
    private final AccountDAO accountDAO;
    private final DeviceDAO deviceDAO;
    private final DeviceAdminDAO deviceAdminDAO;

    public PillResource(final AccountDAO accountDAO, final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB, final DeviceDAO deviceDAO, final DeviceAdminDAO deviceAdminDAO) {
        this.accountDAO = accountDAO;
        this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.deviceAdminDAO = deviceAdminDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/heartbeats")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<PillHeartBeat>> getPillStatus(@Auth final AccessToken accessToken,
                                            @QueryParam("email") final String email,
                                            @QueryParam("pill_id_partial") final String pillIdPartial,
                                            @QueryParam("start_ts") final Long startTs) {

        final DateTime cursor = (startTs == null) ? DateTime.now(DateTimeZone.UTC) : new DateTime(startTs, DateTimeZone.UTC);

        final List<DeviceAccountPair> pills = Lists.newArrayList();
        if (email == null && pillIdPartial == null){
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing query params!").build());
        }

        if (email != null) {
            LOGGER.debug("Querying all pills for email = {}", email);
            final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
            if (!accountIdOptional.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Account not found!").build());
            }
            pills.addAll(deviceDAO.getPillsForAccountId(accountIdOptional.get()));
        } else {
            LOGGER.debug("Querying all pills whose IDs contain = {}", pillIdPartial);
            pills.addAll(deviceAdminDAO.getPillsByPillIdHint(pillIdPartial));
        }

        final Map<String, List<PillHeartBeat>> heartBeatsPerPill = Maps.newHashMap();
        for (final DeviceAccountPair pair : pills) {
            heartBeatsPerPill.put(pair.externalDeviceId, pillHeartBeatDAODynamoDB.get(pair.externalDeviceId, cursor));
        }

        return heartBeatsPerPill;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/heartbeat/{pill_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PillHeartBeat getPillHeartBeat(@Auth final AccessToken accessToken,
                                         @PathParam("pill_id") final String pillId) {


        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getInternalPillId(pillId);
        if(!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("No pill found!").build());
        }

        final Optional<PillHeartBeat> pillHeartBeatOptional = pillHeartBeatDAODynamoDB.get(pillId);
        if(pillHeartBeatOptional.isPresent()) {
            LOGGER.info("Got heartbeat from Dynamo for pill {}", pillId);
            return pillHeartBeatOptional.get();
        }

        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                .entity("No heartbeat found!").build());
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/latest/pair")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceAccountPair> getLatestUniqueActivePillPairs(@Auth final AccessToken accessToken,
                                                                  @QueryParam("max_id") @DefaultValue("1000000") final Integer maxId,
                                                                  @QueryParam("limit") @DefaultValue("100") final Integer limit) {

        return deviceAdminDAO.getLatestUniqueActivePillPairs(maxId, limit);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/latest/data")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PillAdmin> getLatestUniqueActivePillData(@Auth final AccessToken accessToken,
                                                         @QueryParam("max_id") @DefaultValue("1000000") final Integer maxId,
                                                         @QueryParam("limit") @DefaultValue("100") final Integer limit) {

        final List<PillAdmin> pillAdmins = Lists.newArrayList();
        final List<DeviceAccountPair> pillAccountPairs = deviceAdminDAO.getLatestUniqueActivePillPairs(maxId, limit);
        for (final DeviceAccountPair pillAccountPair : pillAccountPairs) {
            pillAdmins.add(new PillAdmin(
                pillAccountPair,
                pillHeartBeatDAODynamoDB.get(pillAccountPair.externalDeviceId, pillAccountPair.created)
            ));
        }
        return  pillAdmins;
    }
}
