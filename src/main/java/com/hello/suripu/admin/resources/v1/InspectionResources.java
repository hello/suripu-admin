package com.hello.suripu.admin.resources.v1;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.oauth.AccessToken;
import com.hello.suripu.admin.oauth.Auth;
import com.hello.suripu.core.db.DeviceDAOAdmin;
import com.hello.suripu.core.models.Account;
import com.codahale.metrics.annotation.Timed;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v1/inspection")
public class InspectionResources {

    private static final Integer MAX_INSPECTED_POPULATION = 1000;
    private static final Integer DEFAULT_INSPECTED_POPULATION = 100;
    private static final Integer DEFAULT_CRITICAL_BATTERY_LEVEL = 20;

    private final DeviceDAOAdmin deviceDAOAdmin;

    public InspectionResources(final DeviceDAOAdmin deviceDAOAdmin) {
        this.deviceDAOAdmin = deviceDAOAdmin;
    }


    @RolesAllowed({"ADMINISTRATION_READ"})
    @GET
    @Timed
    @Path("/sense_without_pill")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Account> getUsersWithSenseWithoutPill(@Auth final AccessToken accessToken,
                                                               @QueryParam("limit") final Integer limit) {
        if (limit == null) {
            return deviceDAOAdmin.getAccountsWithSenseWithoutPill(DEFAULT_INSPECTED_POPULATION);
        }
        return deviceDAOAdmin.getAccountsWithSenseWithoutPill(Math.min(MAX_INSPECTED_POPULATION, limit));
    }


    @RolesAllowed({"ADMINISTRATION_READ"})
    @GET
    @Timed
    @Path("/low_battery_pill")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Account> getInactiveUsers(@Auth final AccessToken accessToken,
                                                   @QueryParam("battery") final Integer battery,
                                                   @QueryParam("limit") final Integer limit){

        final Integer criticalBatteryLevel = (battery == null) ? DEFAULT_CRITICAL_BATTERY_LEVEL : battery;
        final Integer inspectedPopulation = (limit == null) ? DEFAULT_INSPECTED_POPULATION : Math.min(MAX_INSPECTED_POPULATION, limit);

        return deviceDAOAdmin.getAccountsWithLowPillBattery(criticalBatteryLevel, inspectedPopulation);
    }

}
