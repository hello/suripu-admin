package com.hello.suripu.admin.resources.v1;


import com.google.common.base.Optional;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.util.JsonError;
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
import java.util.Map;
import java.util.Set;

@Path("/v1/calibration")
public class CalibrationResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalibrationResources.class);

    public CalibrationDAO calibrationDAO;

    public CalibrationResources(final CalibrationDAO calibrationDAO) {
        this.calibrationDAO = calibrationDAO;
    }

    @GET
    @Path("/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Calibration getCalibration(@PathParam("sense_id")  @NotNull @Valid final String senseId) {
        final Optional<Calibration> optionalCalibration = calibrationDAO.getStrict(senseId);
        if (!optionalCalibration.isPresent()) {
            throw new WebApplicationException(Response.status(404).entity(new JsonError(404, "Calibration not found")).build());
        }
        return optionalCalibration.get();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Calibration> getCalibrationBatch(final Set<String> senseIds,
                                                        @QueryParam("strict") @Nullable @DefaultValue("true") final Boolean strict) {
        return strict ? calibrationDAO.getBatchStrict(senseIds) : calibrationDAO.getBatch(senseIds);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response putCalibration(final Calibration calibration,
                                   @QueryParam("force") @Nullable @DefaultValue("false") final Boolean force) {

        final Boolean hasSuccessfullyUpdated = force ? calibrationDAO.putForce(calibration) : calibrationDAO.put(calibration);

        if (!hasSuccessfullyUpdated){
            throw new WebApplicationException(Response.status(500).entity(new JsonError(500, "Cannot update item, either update condition failed or unexpected aws error occurred")).build());
        }
        return Response.noContent().build();

    }

    @DELETE
    @Path("/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCalibration(@PathParam("sense_id") @NotNull @Valid final String senseId) {
        final Boolean hasSuccessfullyDeleted = calibrationDAO.delete(senseId);
        if (!hasSuccessfullyDeleted) {
            throw new WebApplicationException(Response.status(500).entity(new JsonError(500, "Cannot delete item, either key was incorrect or unexpected aws error occurred")).build());
        }
        return Response.noContent().build();
    }
}
