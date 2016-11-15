package com.hello.suripu.admin.resources.v1;

import com.hello.suripu.admin.db.ExpansionsAdminDAO;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/expansions")
public class ExpansionsResource {

    private final ExpansionsAdminDAO expansionsAdminDAO;

    public ExpansionsResource(final ExpansionsAdminDAO expansionsAdminDAO) {
        this.expansionsAdminDAO = expansionsAdminDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{sense_id}")
    public List<String> expansions(
            @Auth AccessToken accessToken,
            @PathParam("sense_id")final String senseId) {
            return expansionsAdminDAO.getActiveExpansions(senseId);
    }
}
