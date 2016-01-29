package com.hello.suripu.admin.store;

import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/store/")
public class StoreResources {

    private final StoreDAO storeDAO;

    public StoreResources(final StoreDAO storeDAO) {
        this.storeDAO = storeDAO;
    }

    @ScopesAllowed({OAuthScope.STORE_READ})
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Order> search(@Auth final AccessToken accessToken, @QueryParam("q") final String query) {
        return storeDAO.searchOrder(query); // should be sql injection safe
    }


    @ScopesAllowed({OAuthScope.STORE_READ})
    @GET
    @Path("/payment_updates/{order_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PaymentUpdate> paymentUpdates(@Auth final AccessToken accessToken, @PathParam("order_id") final String orderId) {
        return storeDAO.paymentUpdates(orderId); // should be sql injection safe
    }
}
