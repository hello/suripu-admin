package com.hello.suripu.admin.resources.v1;


import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.db.AccessTokenAdminDAO;
import com.hello.suripu.admin.models.AccessTokenAdmin;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessTokenUtils;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.ClientAuthenticationException;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.ImplicitTokenRequest;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.TokenExpirationRequest;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.coredw8.db.AccessTokenDAO;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/token")
public class TokenResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenResources.class);
    private final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> implicitTokenStore;
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;
    private final AccessTokenDAO accessTokenDAO;
    private final AccountDAO accountDAO;
    private final AccessTokenAdminDAO  accessTokenAdminDAO;

    @Context
    HttpServletRequest request;
    public TokenResources(final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> implicitTokenStore,
                          final ApplicationStore<Application, ApplicationRegistration> applicationStore,
                          final AccessTokenDAO accessTokenDAO,
                          final AccountDAO accountDAO,
                          final AccessTokenAdminDAO accessTokenAdminDAO) {

        this.implicitTokenStore = implicitTokenStore;
        this.applicationStore = applicationStore;
        this.accessTokenDAO = accessTokenDAO;
        this.accountDAO = accountDAO;
        this.accessTokenAdminDAO = accessTokenAdminDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @POST
    @Path("/expiration")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getExpiration(@Auth final AccessToken accessToken,
                                 @Valid @NotNull TokenExpirationRequest tokenExpirationRequest) {
        final String dirtyToken = tokenExpirationRequest.dirtyToken;
        final Optional<UUID> tokenUUIDOptional = AccessTokenUtils.cleanUUID(dirtyToken);
        if(!tokenUUIDOptional.isPresent()) {
            LOGGER.warn("Invalid format for token {}", dirtyToken);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        final Optional<AccessToken> accessTokenOptional = accessTokenDAO.getByAccessToken(tokenUUIDOptional.get());
        if (!accessTokenOptional.isPresent()) {
            LOGGER.warn("Token {} not found", dirtyToken);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        return Days.daysBetween(DateTime.now(DateTimeZone.UTC), accessTokenOptional.get().createdAt.plusSeconds((int) accessTokenOptional.get().expiresIn.longValue())).getDays();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/active")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccessTokenAdmin> getActiveTokens(@Auth final AccessToken accessToken,
                                                  @QueryParam("max_id") final Long maxId,
                                                  @QueryParam("limit") @DefaultValue("50") final Integer limit){
        if (maxId == null) {
            return accessTokenAdminDAO.getMostRecentActiveTokens(limit);
        }
        return accessTokenAdminDAO.getActiveTokensWithCursor(maxId, limit);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccessTokenAdmin> getTokenByAccountId(@Auth final AccessToken accessToken,
                                                      @QueryParam("email") final String email,
                                                      @QueryParam("limit") @DefaultValue("100") final Integer limit){
        if(email == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        return accessTokenAdminDAO.getActiveTokensByAccountId(accountIdOptional.get(), limit);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Path("/invalidate/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeToken(@Auth final AccessToken accessToken,
                                @PathParam("id") final Long id){

        final Integer rowAffected = accessTokenAdminDAO.updateExpiration(id, 0);

        if (rowAffected == 0) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return Response.noContent().build();
    }


    @ScopesAllowed({OAuthScope.IMPLICIT_TOKEN})
    @POST
    @Path("/implicit")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AccessToken accessToken(@Auth final AccessToken accessToken,
                                   @Valid @NotNull final ImplicitTokenRequest implicitTokenRequest) {
        LOGGER.debug("Raw implicit token request {}", implicitTokenRequest);
        String requesterEmail = this.request.getHeader(HelloHttpHeader.ADMIN);

        if (requesterEmail == null || !requesterEmail.endsWith("@sayhello.com")) {
            LOGGER.error("Unauthorized attempt to generate token from email {}", requesterEmail);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final Optional<Account> accountOptional = accountDAO.getByEmail(implicitTokenRequest.email);
        if (!accountOptional.isPresent()) {
            LOGGER.error("Account not found for email {}", implicitTokenRequest.email);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        final Long accountId = accountOptional.get().id.get();

        LOGGER.debug("Admin {} attempts to generate token on behalf of account {} - email {}", requesterEmail, accountId, implicitTokenRequest.email);


        final Optional<Application> applicationOptional = applicationStore.getApplicationByClientId(implicitTokenRequest.clientId);
        if (!applicationOptional.isPresent()) {
            LOGGER.error("application wasn't found for clientId : {}", implicitTokenRequest.clientId);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final Application application = applicationOptional.get();
        if (!application.hasScope(OAuthScope.AUTH)) {
            LOGGER.error("application does not have proper scope : {}", implicitTokenRequest.clientId);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        final ClientDetails details = ClientDetails.createWithImplicitGrantType(application, accountId);

        details.setApp(application);
        AccessToken implicitToken;
        try {
            implicitToken = implicitTokenStore.storeAccessToken(details);

        } catch (ClientAuthenticationException e) {
            LOGGER.error("Failed to generate token on behalf of account {} because {}", implicitTokenRequest.email, e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());

        }

        LOGGER.info("Admin {} created implicit token {} for account {}", requesterEmail, implicitToken.toString(), implicitTokenRequest.email);
        return implicitToken;

    }
}
