package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by jyfan on 9/2/15.
 */

@Path("/v1/insightsResource")

public class InsightsResource {

    private final InsightProcessor insightProcessor;
    private final DeviceDAO deviceDAO;

    public InsightsResource(final InsightProcessor insightProcessor, final DeviceDAO deviceDAO) {
        this.insightProcessor = insightProcessor;
        this.deviceDAO = deviceDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping(@Auth final AccessToken accessToken) {
        return Response.noContent().build();
    }



    //TODO: what is the best way of getting a list of accountIds? Should replace with endpoint for triggering for single account?
    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<Long> generateInsight(@Auth final AccessToken accessToken, //how to get AccessToken?
                                                           final Set<Long> accountIds,
                                                           @QueryParam("category") final InsightCard.Category category) {

        final ArrayList<Long> accountsWithGeneratedInsights = Lists.newArrayList();
        for (final Long accountId : accountIds) {

            final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
            if (!deviceIdOptional.isPresent()) {
                continue;
            }
            final Long deviceId = deviceIdOptional.get();

            insightProcessor.generateInsightsByCategory(accountId, deviceId, category);
            //TODO: uncomment below when InsightProcessor gets merged & deployed
//            final Optional<InsightCard.Category> generatedInsight = insightProcessor.generateInsightsByCategory(accountId, deviceId, category);
//            if (generatedInsight.isPresent()) {
//                accountsWithGeneratedInsights.add(accountId);
//            }
        }

        return accountsWithGeneratedInsights;
    }
}
