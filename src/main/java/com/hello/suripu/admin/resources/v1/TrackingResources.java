package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.admin.processors.ActiveDevicesTracker;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/v1/tracking")
public class TrackingResources {
    private ActiveDevicesTracker activeDevicesTracker;

    public TrackingResources(final ActiveDevicesTracker activeDevicesTracker) {
        this.activeDevicesTracker = activeDevicesTracker;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/active_devices_diff/{device_type}")
    public Set<String> getDiffHourlyActiveDevices(@Auth final AccessToken accessToken,
                                                  @PathParam("device_type") final String deviceType,
                                                  @QueryParam("before") final String beforeDateTimeString,
                                                  @QueryParam("after") final String afterDateTimeString) {

        if (beforeDateTimeString == null || afterDateTimeString == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        final String beforeSetKey = String.format("hourly_active_%s_%s", deviceType, beforeDateTimeString);
        final String afterSetKey = String.format("hourly_active_%s_%s", deviceType, afterDateTimeString);

        final Optional<Set<String>> diffDevicesOptional = activeDevicesTracker.getDiff(beforeSetKey, afterSetKey);
        if (!diffDevicesOptional.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return diffDevicesOptional.get();
    }
}
