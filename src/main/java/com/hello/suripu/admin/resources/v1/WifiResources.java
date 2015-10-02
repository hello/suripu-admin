package com.hello.suripu.admin.resources.v1;


import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.WifiInfoDynamoDB;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

@Path("/v1/wifi")
public class WifiResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(WifiResources.class);

    private final WifiInfoDAO wifiInfoDAO;

    public WifiResources(final WifiInfoDAO wifiInfoDAO) {
        this.wifiInfoDAO = wifiInfoDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{sense_id}")
    public WifiInfo retrieveWifiInfo(@Auth final AccessToken token,
                                     @PathParam("sense_id") final String senseId ){

        final Optional<WifiInfo> wifiInfoOptional = wifiInfoDAO.get(senseId);

        if (!wifiInfoOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(),
                            String.format("Wifi info not found for %s", senseId))).build());
        }

        return wifiInfoOptional.get();
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @Timed
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putWifiInfo (@Auth final AccessToken token,
                                 @Valid @NotNull final WifiInfo wifiInfo){

        final Boolean hasPut = wifiInfoDAO.put(wifiInfo);

        if (!hasPut) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            String.format("Failed to update wifi info for sense %s", wifiInfo.senseId))).build());
        }

        return Response.noContent().build();
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @Timed
    @PUT
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putBatchWifiInfo (@Auth final AccessToken token,
                                      @Valid @NotNull final List<WifiInfo> wifiInfoList){

        if (wifiInfoList.size() > WifiInfoDynamoDB.MAXIMUM_BATCH_WRITE_SIZE) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(),
                            String.format("Max batch size allowed is %s", WifiInfoDynamoDB.MAXIMUM_BATCH_WRITE_SIZE))).build());
        }

        final Boolean hasPut = wifiInfoDAO.putBatch(wifiInfoList);

        if (!hasPut) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            String.format("Failed to update wifi info batch"))).build());
        }

        return Response.noContent().build();
    }
}
