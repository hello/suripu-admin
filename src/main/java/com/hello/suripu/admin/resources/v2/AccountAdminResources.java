package com.hello.suripu.admin.resources.v2;


import com.google.common.base.Optional;
import com.hello.suripu.admin.models.AccountAdminView;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/account")
public class AccountAdminResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountAdminResources.class);

    public final AccountDAO accountDAO;

    public AccountAdminResources(final AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccountAdminView retrieveAccountByEmailOrId(@Auth final AccessToken accessToken,
                                              @QueryParam("email") final String email,
                                              @QueryParam("id") final Long id) {
        if (email == null &&  id == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Missing query params, please specify email or id").build());
        }

        final Optional<Account> account = (email !=null)
                ? accountDAO.getByEmail(email.toLowerCase())
                : accountDAO.getById(id);

        if (!account.isPresent()) {
            LOGGER.warn("email={} id={} result=missing", email, id);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Account not found").build());
        }

        return AccountAdminView.fromAccount(account.get());
    }
}
