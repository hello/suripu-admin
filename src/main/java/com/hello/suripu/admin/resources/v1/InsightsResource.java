package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.admin.models.InsightsGenerationRequest;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            LOGGER.debug("Could not get deviceId, no {} insight generated for accountId {}", category, accountId);
            return Optional.absent();
        }

        final Long deviceId = deviceIdOptional.get();

        final Optional<InsightCard.Category> generatedInsight = insightProcessor.generateInsightsByCategory(accountId, DeviceId.create(deviceId), deviceDataInsightQueryDAO, category);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("Could not generate {} insight for accountId {} ", category, accountId);
            return Optional.absent();
        }

        LOGGER.debug("Successfully generated {} insight for accountId {}", category, accountId);
        return generatedInsight;
    }

}
