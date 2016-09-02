package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

@Path("/v1/key_store")
public class KeyStoreResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreResources.class);

    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;

    public KeyStoreResources(final KeyStore senseKeyStore,
                             final KeyStore pillKeyStore) {

        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/sense/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceKeyStoreRecord getSenseKeyStore(@Auth final AccessToken accessToken,
                                                 @PathParam("sense_id") final String senseId) {
        final Optional<DeviceKeyStoreRecord> senseKeyStoreRecord = senseKeyStore.getKeyStoreRecord(senseId);
        if (!senseKeyStoreRecord.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This sense has not been properly provisioned!").build());
        }
        return senseKeyStoreRecord.get();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @POST
    @Timed
    @Path("/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, DeviceKeyStoreRecord> getSenseKeyStoreByBatch(@Auth final AccessToken accessToken,
                                                                     final Set<String> senseIds) {
        return senseKeyStore.getKeyStoreRecordBatch(senseIds);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/pill/{pill_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceKeyStoreRecord getPillKeyStore(@Auth final AccessToken accessToken,
                                                @PathParam("pill_id") final String pillId) {
        final Optional<DeviceKeyStoreRecord> pillKeyStoreRecord = pillKeyStore.getKeyStoreRecord(pillId);
        if (!pillKeyStoreRecord.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This pill has not been properly provisioned!").build());
        }
        return pillKeyStoreRecord.get();
    }




    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @POST
    @Timed
    @Path("/pill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, DeviceKeyStoreRecord> getPillKeyStoreByBatch(@Auth final AccessToken accessToken,
                                                                    final Set<String> pillIds) {
        return pillKeyStore.getKeyStoreRecordBatch(pillIds);
    }
}
