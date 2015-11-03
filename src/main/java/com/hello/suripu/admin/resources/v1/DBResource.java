package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.db.TableDAO;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/db/check")
public class DBResource {

    private TableDAO tableDAO;

    public DBResource(final TableDAO tableDAO) {
        this.tableDAO = tableDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Path("/sensors")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<String> getTables(@Auth final AccessToken token) {
        return tableDAO.tables();
    }
}
