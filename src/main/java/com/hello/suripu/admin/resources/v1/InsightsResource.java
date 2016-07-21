package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.admin.models.InsightsGenerationRequest;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jyfan on 9/2/15.
 */

@Path("/v1/insights")

public class InsightsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsResource.class);

    private final InsightProcessor insightProcessor;
    private final DeviceDAO deviceDAO;
    private final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO;

    public InsightsResource(final InsightProcessor insightProcessor, final DeviceDAO deviceDAO, final DeviceDataInsightQueryDAO deviceDataInsightQueryDAO ) {
        this.insightProcessor = insightProcessor;
        this.deviceDAO = deviceDAO;
        this.deviceDataInsightQueryDAO = deviceDataInsightQueryDAO;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateInsight")
    public Optional<InsightCard.Category> generateInsight(@Auth final AccessToken accessToken,
                                                               InsightsGenerationRequest generateInsightRequest) {

        final InsightCard.Category category = generateInsightRequest.insightCategory;
        final Long accountId = generateInsightRequest.accountId;



        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            LOGGER.debug("action=no-insight reason=device-account-pair-absent accountId={} insight_cat={}", accountId, category.toString());
            return Optional.absent();
        }

        final Optional<InsightCard.Category> generatedInsight = insightProcessor.generateInsightsByCategory(accountId, deviceAccountPairOptional.get(), deviceDataInsightQueryDAO, category);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("action=no-insight accountId={} insight_cat={}", accountId, category.toString());
            return Optional.absent();
        }

        LOGGER.debug("action=insight-generated accountId={} insight_cat={}", accountId, category.toString());
        return generatedInsight;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/categories")
    public List<InsightCard.Category> getCategories(@Auth final AccessToken accessToken){
        return Arrays.asList(InsightCard.Category.values());
    }
}
