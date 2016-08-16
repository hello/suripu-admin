package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.admin.models.AggStatsGenerationRequest;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggStatsDAODynamoDB;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.AggStatsProcessor;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by jyfan on 7/25/16.
 */

@Path("/v1/aggstats")

public class AggStatsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggStatsResource.class);

    private final AccountDAO accountDAO;
    private final AggStatsProcessor aggStatsProcessor;
    private final AggStatsDAODynamoDB aggStatsDAODynamoDB;
    private final DeviceReadDAO deviceReadDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;

    public AggStatsResource(final AccountDAO accountDAO,
                            final AggStatsProcessor aggStatsProcessor,
                            final AggStatsDAODynamoDB aggStatsDAODynamoDB,
                            final DeviceReadDAO deviceReadDAO,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        this.accountDAO = accountDAO;
        this.aggStatsProcessor = aggStatsProcessor;
        this.aggStatsDAODynamoDB = aggStatsDAODynamoDB;
        this.deviceReadDAO = deviceReadDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateAggStats")
    public Integer generateSingleAggStats(@Auth final AccessToken accessToken,
                                          final AggStatsGenerationRequest aggStatsGenerationRequest) {

        final Long accountId = aggStatsGenerationRequest.accountId;

        //target date?
        final DateTime startDateLocalInclusive = aggStatsGenerationRequest.startDateLocalInclusive;
        final DateTime endDateLocalInclusive = aggStatsGenerationRequest.endDateLocalInclusive;
        final DateTime endDateLocalExclusive = endDateLocalInclusive.plusDays(1);

        //overwrite?
        final Boolean overwrite = aggStatsGenerationRequest.overwrite;

        //get sense device id
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceReadDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            LOGGER.debug("action=no-aggstats reason=device-account-pair-absent request={}", aggStatsGenerationRequest.toString());
            return 0;
        }
        final DeviceAccountPair deviceAccountPair = deviceAccountPairOptional.get();

        //get timezone offset
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("action=no-aggstats reason=timezoneoffset-absent request={}", aggStatsGenerationRequest.toString());
            return 0;
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        //generate agg stat for target date
        Integer numSuccesses = 0;
        for (DateTime targetDateLocal = startDateLocalInclusive; targetDateLocal.isBefore(endDateLocalExclusive); targetDateLocal = targetDateLocal.plusDays(1)) {
            final Boolean successGeneration = aggStatsProcessor.generatePastAggStat(deviceAccountPair, targetDateLocal, timeZoneOffset, overwrite);
            LOGGER.info("action=gened-aggstats accountId={} target_date_local={} overwrite={} success={}", accountId, targetDateLocal.toString(), overwrite.toString(), successGeneration);
            if (successGeneration) {
                numSuccesses += 1;
            }
        }

        return numSuccesses;
    }

}
