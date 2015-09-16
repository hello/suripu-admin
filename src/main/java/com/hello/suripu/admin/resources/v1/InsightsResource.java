package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by jyfan on 9/2/15.
 */

@Path("/v1/insights")

public class InsightsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsResource.class);

    private final InsightProcessor insightProcessor;
    private final DeviceDAO deviceDAO;

    public InsightsResource(final InsightProcessor insightProcessor, final DeviceDAO deviceDAO) {
        this.insightProcessor = insightProcessor;
        this.deviceDAO = deviceDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-insight-light-for-user")
    public Optional<InsightCard.Category> generateInsightLight(@Auth final AccessToken accessToken,
                                                           @QueryParam("account_id") final Long accountId) {

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            LOGGER.debug("Could not get deviceId, no light insight generated for accountId {}", accountId);
            return Optional.absent();
        }

        final Long deviceId = deviceIdOptional.get();

        final Optional<InsightCard.Category> generatedInsight = this.insightProcessor.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.LIGHT);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("Could not generate light insight for accountId {} ", accountId);
            return Optional.absent();
        }

        LOGGER.debug("Successfully generated light insight for accountId {}", accountId);
        return generatedInsight;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-insight-temperature-for-user")
    public Optional<InsightCard.Category> generateInsightTemperature(@Auth final AccessToken accessToken,
                                                               @QueryParam("account_id") final Long accountId) {

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            LOGGER.debug("Could not get deviceId, no temperature insight generated for accountId {}", accountId);
            return Optional.absent();
        }

        final Long deviceId = deviceIdOptional.get();

        final Optional<InsightCard.Category> generatedInsight = this.insightProcessor.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.TEMPERATURE);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("Could not generate temperature insight for accountId {} ", accountId);
            return Optional.absent();
        }

        LOGGER.debug("Successfully generated temperature insight for accountId {}", accountId);
        return generatedInsight;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-insight-motion-for-user")
    public Optional<InsightCard.Category> generateInsightMotion(@Auth final AccessToken accessToken,
                                                               @QueryParam("account_id") final Long accountId) {

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            LOGGER.debug("Could not get deviceId, no motion/sleep-quality insight generated for accountId {}", accountId);
            return Optional.absent();
        }

        final Long deviceId = deviceIdOptional.get();

        final Optional<InsightCard.Category> generatedInsight = this.insightProcessor.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.SLEEP_QUALITY);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("Could not generate motion/sleep-quality insight for accountId {} ", accountId);
            return Optional.absent();
        }

        LOGGER.debug("Successfully generated motion/sleep-quality insight for accountId {}", accountId);
        return generatedInsight;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-insight-wake-variance-for-user")
    public Optional<InsightCard.Category> generateInsightWakeVariance(@Auth final AccessToken accessToken,
                                                               @QueryParam("account_id") final Long accountId) {

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            LOGGER.debug("Could not get deviceId, no wake-variance insight generated for accountId {}", accountId);
            return Optional.absent();
        }

        final Long deviceId = deviceIdOptional.get();

        final Optional<InsightCard.Category> generatedInsight = this.insightProcessor.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.WAKE_VARIANCE);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("Could not generate wake variance insight for accountId {} ", accountId);
            return Optional.absent();
        }

        LOGGER.debug("Successfully generated wake variance insight for accountId {}", accountId);
        return generatedInsight;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-insight-bed-light-duration-for-user")
    public Optional<InsightCard.Category> generateInsightBedLightDuration(@Auth final AccessToken accessToken,
                                                               @QueryParam("account_id") final Long accountId) {

        final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (!deviceIdOptional.isPresent()) {
            LOGGER.debug("Could not get deviceId, no bed-light-duration insight generated for accountId {}", accountId);
            return Optional.absent();
        }

        final Long deviceId = deviceIdOptional.get();

        final Optional<InsightCard.Category> generatedInsight = this.insightProcessor.generateInsightsByCategory(accountId, deviceId, InsightCard.Category.BED_LIGHT_DURATION);

        if (!generatedInsight.isPresent()) {
            LOGGER.debug("Could not generate bed light duration insight for accountId {} ", accountId);
            return Optional.absent();
        }

        LOGGER.debug("Successfully generated bed light duration insight for accountId {}", accountId);
        return generatedInsight;
    }


}
