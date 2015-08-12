package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.admin.oauth.AccessToken;
import com.hello.suripu.admin.oauth.Auth;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.Team;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Path("/v1/teams")
public class TeamsResources {

    private final TeamStore teamStore;

    public TeamsResources(final TeamStore teamStore) {
        this.teamStore = teamStore;
    }

    @RolesAllowed({"ADMINISTRATION_READ"})
    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Team> allDeviceTeams(@Auth final AccessToken accessToken) {
        return teamStore.getTeams(TeamStore.Type.DEVICES);
    }

    @RolesAllowed({"ADMINISTRATION_READ"})
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Team> allUsersTeams(@Auth final AccessToken accessToken) {
        return teamStore.getTeams(TeamStore.Type.USERS);
    }

    @RolesAllowed({"ADMINISTRATION_READ"})
    @GET
    @Path("/devices/{team_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Team getDeviceTeam(@Auth final AccessToken accessToken, @PathParam("team_name") String teamName) {

        final Optional<Team> team = teamStore.getTeam(teamName, TeamStore.Type.DEVICES);
        if(team.isPresent()) {
            return team.get();
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @RolesAllowed({"ADMINISTRATION_READ"})
    @GET
    @Path("/users/{team_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Team getUsersTeam(@Auth final AccessToken accessToken, @PathParam("team_name") String teamName) {
        final Optional<Team> team = teamStore.getTeam(teamName, TeamStore.Type.USERS);
        if(team.isPresent()) {
            return team.get();
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @RolesAllowed({"ADMINISTRATION_READ"})
    @PUT
    @Path("/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createDeviceTeam(@Auth final AccessToken accessToken, @Valid final Team team) {
        teamStore.createTeam(team, TeamStore.Type.DEVICES);
    }

    @RolesAllowed({"ADMINISTRATION_READ"})
    @PUT
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createUsersTeam(@Auth final AccessToken accessToken, @Valid final Team team) {
        teamStore.createTeam(team, TeamStore.Type.USERS);
    }

    @RolesAllowed({"ADMINISTRATION_WRITE"})
    @POST
    @Path("/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addToDevicesTeam(
            @Auth final AccessToken accessToken,
            @Valid final Team team) {
        teamStore.add(team.name, TeamStore.Type.DEVICES, Lists.newArrayList(team.ids));
    }

    @RolesAllowed({"ADMINISTRATION_WRITE"})
    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addToUsersTeam(@Auth final AccessToken accessToken,
                               @Valid final Team team) {
        teamStore.add(team.name, TeamStore.Type.USERS, Lists.newArrayList(team.ids));
    }

    @RolesAllowed({"ADMINISTRATION_WRITE"})
    @DELETE
    @Path("/devices/{team_name}")
    public void deleteDevicesTeam(
            @Auth final AccessToken accessToken,
            @PathParam("team_name") final String teamName) {
        final Team team = Team.create(teamName, new HashSet<String>());
        teamStore.delete(team, TeamStore.Type.DEVICES);
    }

    @RolesAllowed({"ADMINISTRATION_WRITE"})
    @DELETE
    @Path("/users/{team_name}")
    public void deleteUsersTeam(
            @Auth final AccessToken accessToken,
            @PathParam("team_name") final String teamName) {
        final Team team = Team.create(teamName, new HashSet<String>());
        teamStore.delete(team, TeamStore.Type.USERS);
    }

    @RolesAllowed({"ADMINISTRATION_WRITE"})
    @DELETE
    @Path("/devices/{team_name}/{device_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeFromDevicesTeam(
            @Auth final AccessToken accessToken,
            @PathParam("team_name") final String teamName,
            @PathParam("device_id") final String deviceId){
        final List<String> ids = new ArrayList<>();
        ids.add(deviceId);
        teamStore.remove(teamName, TeamStore.Type.DEVICES, ids);
    }

    @RolesAllowed({"ADMINISTRATION_WRITE"})
    @DELETE
    @Path("/users/{team_name}/{user_id}")
    public void removeFromUsersTeam(
            @Auth final AccessToken accessToken,
            @PathParam("team_name") final String teamName,
            @PathParam("user_id") final Long userId) {
        final List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(userId));
        teamStore.remove(teamName, TeamStore.Type.USERS, ids);
    }
}
