package com.hello.suripu.admin.resources.v1;


import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import io.dropwizard.jersey.caching.CacheControl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/version")
public class VersionResources {
    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    public Response getVersion(@Auth final AccessToken accessToken) {
        return Response.ok(this.getClass().getPackage().getImplementationVersion())
                .type(MediaType.TEXT_PLAIN).build();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/core")
    @Produces(MediaType.TEXT_PLAIN)
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    public Response getCoreVersion(@Auth final AccessToken accessToken) {
        return Response.ok(accessToken.getClass().getPackage().getImplementationVersion())
                .type(MediaType.TEXT_PLAIN).build();
    }
}