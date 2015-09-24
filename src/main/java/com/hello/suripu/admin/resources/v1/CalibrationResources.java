package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/v1/calibration")
public class CalibrationResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalibrationResources.class);
    private final CalibrationDAO calibrationDAO;

    public CalibrationResources(final CalibrationDAO calibrationDAO) {
        this.calibrationDAO = calibrationDAO;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Calibration getCalibration(@Auth final AccessToken accessToken, @PathParam("sense_id")  @NotNull @Valid final String senseId) {
        final Optional<Calibration> optionalCalibration = calibrationDAO.getStrict(senseId);
        if (!optionalCalibration.isPresent()) {
            throw new WebApplicationException(Response.status(404).entity(new JsonError(404, "Calibration not found")).build());
        }
        return optionalCalibration.get();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Calibration> getCalibrationBatch(@Auth final AccessToken accessToken,
                                                        final Set<String> senseIds,
                                                        @QueryParam("strict") @Nullable @DefaultValue("true") final Boolean strict) {
        return strict ? calibrationDAO.getBatchStrict(senseIds) : calibrationDAO.getBatch(senseIds);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response putCalibration(@Auth final AccessToken accessToken,
                                   final Calibration calibration,
                                   @QueryParam("force") @Nullable @DefaultValue("false") final Boolean force) {

        if (force) {
            final Boolean hasPutForceSuccessfully = calibrationDAO.putForce(calibration);
            if (!hasPutForceSuccessfully){
                throw new WebApplicationException(Response.status(500).entity(new JsonError(500, "Failed to put force")).build());
            }
        } else {
            final Optional<Boolean> hasPutSuccessfully = calibrationDAO.put(calibration);
            if (!hasPutSuccessfully.isPresent()) {
                throw new WebApplicationException(Response.status(400).entity(new JsonError(400, "Failed to put as condition was not satisfied")).build());
            }
            if (!hasPutSuccessfully.get()) {
                throw new WebApplicationException(Response.status(500).entity(new JsonError(500, "Failed to put")).build());
            }
        }

        return Response.noContent().build();
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> putCalibration(@Auth final AccessToken accessToken,
                                              final List<Calibration> calibrations,
                                              @QueryParam("force") @Nullable @DefaultValue("false") final Boolean force) {

        final Map<String, String> putBatchResults = Maps.newHashMap();

        if (force) {
            if (calibrations.size() > CalibrationDynamoDB.MAX_PUT_FORCE_SIZE){
                throw new WebApplicationException(Response.status(400).entity(new JsonError(400, String.format("Batch size shhould be less than %s", CalibrationDynamoDB.MAX_PUT_FORCE_SIZE))).build());
            }
            final Map<String, Boolean> putBatchResponse = calibrationDAO.putBatchForce(calibrations);

            for (final Map.Entry<String, Boolean> item :  putBatchResponse.entrySet()) {
                putBatchResults.put(item.getKey(), item.getValue().toString());
            }
        } else {
            if (calibrations.size() > CalibrationDynamoDB.MAX_PUT_SIZE){
                throw new WebApplicationException(Response.status(400).entity(new JsonError(400, String.format("Batch size shhould be less than %s", CalibrationDynamoDB.MAX_PUT_SIZE))).build());
            }
            final Map<String, Optional<Boolean>> putBatchResponse = calibrationDAO.putBatch(calibrations);

            for (final Map.Entry<String, Optional<Boolean>> item :  putBatchResponse.entrySet()) {
                if (!item.getValue().isPresent()) {
                    putBatchResults.put(item.getKey(), "Condition was not satisfied");
                }
                else {
                    putBatchResults.put(item.getKey(), item.getValue().get().toString());
                }
            }
        }

        return putBatchResults;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Path("/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCalibration(@Auth final AccessToken accessToken,
                                      @PathParam("sense_id") @NotNull @Valid final String senseId) {
        final Boolean hasSuccessfullyDeleted = calibrationDAO.delete(senseId);
        if (!hasSuccessfullyDeleted) {
            throw new WebApplicationException(Response.status(500).entity(new JsonError(500, "Cannot delete item, either key was incorrect or unexpected aws error occurred")).build());
        }
        return Response.noContent().build();
    }
}
