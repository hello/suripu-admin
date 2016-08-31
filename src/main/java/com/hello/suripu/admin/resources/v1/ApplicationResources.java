package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.OAuthScope;

import com.hello.suripu.core.oauth.stores.ApplicationStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
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
import java.util.List;

@Path("/v1/applications")
public class ApplicationResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationResources.class);
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;

    public ApplicationResources(final ApplicationStore<Application, ApplicationRegistration> applicationStore) {
        this.applicationStore = applicationStore;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Auth final AccessToken token,
                             @Valid final ApplicationRegistration applicationRegistration) {
        final ApplicationRegistration applicationWithDevAccountId = ApplicationRegistration.addDevAccountId(applicationRegistration, token.accountId);
        applicationStore.register(applicationWithDevAccountId);
        return Response.ok().build();
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/{dev_account_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> getApplicationsByDeveloper(@Auth final AccessToken token,
                                                        @PathParam("dev_account_id") final Long devAccountId) {

        return applicationStore.getApplicationsByDevId(devAccountId);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> getAllApplications(@Auth final AccessToken accessToken) {
        final List<Application> applications = applicationStore.getAll();
        LOGGER.debug("Size of applications = {}", applications.size());
        return applications;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/scopes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OAuthScope> scopes(@Auth final AccessToken accessToken) {
        final List<OAuthScope> scopes = new ArrayList<>();
        for(OAuthScope scope : OAuthScope.values()) {
            scopes.add(scope);
        }
        return scopes;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/{id}/scopes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OAuthScope> scopesForApplication(@Auth final AccessToken accessToken,
                                                 @PathParam("id") final Long applicationId) {
        final Optional<Application> applicationOptional = applicationStore.getApplicationById(applicationId);
        if(!applicationOptional.isPresent()) {

            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<OAuthScope> scopes = new ArrayList<>();

        for(OAuthScope scope : applicationOptional.get().scopes) {
            scopes.add(scope);
        }

        return scopes;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Path("/{id}/scopes")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateScopes(@Auth final AccessToken accessToken,
                             @Valid List<OAuthScope> scopes,
                             @PathParam("id") final Long applicationId) {
        applicationStore.updateScopes(applicationId, scopes);
    }
}
