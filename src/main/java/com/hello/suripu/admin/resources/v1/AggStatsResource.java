package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.admin.db.RedshiftDAO;
import com.hello.suripu.admin.models.AggStatsGenerationRequest;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggStatsDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.AggStatsInputs;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.pill.data.AggStatTrackerMotion;
import com.hello.suripu.core.processors.AggStatsProcessor;
import com.hello.suripu.core.sense.data.AggStatDeviceData;
import com.hello.suripu.core.util.AggStatsComputer;

import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by jyfan on 7/25/16.
 */

@Path("/v1/aggstats")

public class AggStatsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggStatsResource.class);

    private final AccountDAO accountDAO;
    private final AggStatsProcessor aggStatsProcessor;
    private final AggStatsDAODynamoDB aggStatsDAODynamoDB;
    private final CalibrationDAO calibrationDAO;
    private final DeviceReadDAO deviceReadDAO;
    private final RedshiftDAO redshiftDAO;
    private final SenseColorDAO senseColorDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;

    public AggStatsResource(final AccountDAO accountDAO,
                            final AggStatsProcessor aggStatsProcessor,
                            final AggStatsDAODynamoDB aggStatsDAODynamoDB,
                            final CalibrationDAO calibrationDAO,
                            final DeviceReadDAO deviceReadDAO,
                            final RedshiftDAO redshiftDAO,
                            final SenseColorDAO senseColorDAO,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        this.accountDAO = accountDAO;
        this.aggStatsProcessor = aggStatsProcessor;
        this.aggStatsDAODynamoDB = aggStatsDAODynamoDB;
        this.calibrationDAO = calibrationDAO;
        this.deviceReadDAO = deviceReadDAO;
        this.redshiftDAO = redshiftDAO;
        this.senseColorDAO = senseColorDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateAggStatsDynamo")
    public Integer generateAggStatsDynamo(@Auth final AccessToken accessToken,
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

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateAggStats")
    public Integer generateAggStats(@Auth final AccessToken accessToken,
                                    final AggStatsGenerationRequest aggStatsGenerationRequest) {

        final Long accountId = aggStatsGenerationRequest.accountId;

        //get sense device id
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceReadDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            LOGGER.debug("action=no-aggstats reason=device-account-pair-absent request={}", aggStatsGenerationRequest.toString());
            return 0;
        }
        final DeviceAccountPair deviceAccountPair = deviceAccountPairOptional.get();
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);

        //get timezone offset
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("action=no-aggstats reason=timezoneoffset-absent request={}", aggStatsGenerationRequest.toString());
            return 0;
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        //target date?
        final DateTime startDateLocalInclusive = aggStatsGenerationRequest.startDateLocalInclusive;
        final DateTime endDateLocalInclusive = aggStatsGenerationRequest.endDateLocalInclusive;
        final DateTime endDateLocalExclusive = endDateLocalInclusive.plusDays(1);

        final DateTime utcNow = DateTime.now(DateTimeZone.UTC);
        final DateTime localNow = utcNow.plusMillis(timeZoneOffset);
        final DateTime latestAllowedDate = localNow.minusDays(1).withTimeAtStartOfDay();

        if (endDateLocalInclusive.isAfter(latestAllowedDate)) {
            LOGGER.error("method=past-aggstat action=skip-compute-agg-stats reason=target-date-past-latest-allowed account_id={}", accountId);
            return 0;
        }

        //overwrite?
        final Boolean overwrite = aggStatsGenerationRequest.overwrite;

        //Query deviceData & pillData
        final DateTime queryStartTime = startDateLocalInclusive.withHourOfDay(AggStats.DAY_START_END_HOUR);
        final DateTime queryEndTime = endDateLocalExclusive.withHourOfDay(AggStats.DAY_START_END_HOUR);

        final ImmutableList<AggStatDeviceData> aggStatDeviceDataListAll = redshiftDAO.getSenseDataBetweenLocalUTC(accountId, queryStartTime, queryEndTime);
        LOGGER.trace("resource=agg-stats action=queryed-device-data account_id={} aggStatsGenerationRequest={} len_data={}", accountId, aggStatsGenerationRequest.toString(), aggStatDeviceDataListAll.size());

        final ImmutableList<AggStatTrackerMotion> aggStatPillDataListAll = redshiftDAO.getPillDataBetweenLocalUTC(accountId, queryStartTime, queryEndTime);
        LOGGER.trace("resource=agg-stats action=queryed-tracker-motion account_id={} aggStatsGenerationRequest={} len_data={}", accountId, aggStatsGenerationRequest.toString(), aggStatPillDataListAll.size());

        //Query sense color, dust calibration
        final Optional<Device.Color> senseColorOptional = aggStatsProcessor.getSenseColorOptional(senseColorDAO, deviceId);
        final Optional<Calibration> calibrationOptional = aggStatsProcessor.getCalibrationOptional(calibrationDAO, deviceId);

        //generate agg stat for target dates
        Integer numSuccess = 0;
        for (DateTime targetDateLocal = startDateLocalInclusive; targetDateLocal.isBefore(endDateLocalExclusive); targetDateLocal = targetDateLocal.plusDays(1)) {

            //is agg stat already present?
            final Optional<AggStats> presentAggStat = aggStatsDAODynamoDB.getSingleStat(accountId, deviceId, targetDateLocal);
            if (presentAggStat.isPresent() && !overwrite) {
                LOGGER.debug("action=skip-compute-agg-stats condition=agg-stats-already-present account_id={} target_date_local={}", accountId, targetDateLocal.toString());
                continue;
            }

            //generate target date
            final DateTime startLocalTime = targetDateLocal.withHourOfDay(AggStats.DAY_START_END_HOUR);
            final DateTime endLocalTime = targetDateLocal.plusDays(1).withHourOfDay(AggStats.DAY_START_END_HOUR);

            //Extract deviceData
            final ImmutableList<AggStatDeviceData> aggStatDeviceDataList = extractDeviceDataList(aggStatDeviceDataListAll, startLocalTime, endLocalTime);
            LOGGER.trace("resource=agg-stats data=device-data account_id={} targetDateLocal={} len_data={}", accountId, targetDateLocal.toString(), aggStatDeviceDataList.size());

            //Extract pillData
            final ImmutableList<AggStatTrackerMotion> pillDataList = extractTrackerMotionList(aggStatPillDataListAll, startLocalTime, endLocalTime);
            LOGGER.trace("resource=agg-stats data=tracker-motion account_id={} targetDateLocal={} len_data={}", accountId, targetDateLocal.toString(), pillDataList.size());

            //Compute aggregate stats
            final AggStatsInputs aggStatsInputs = AggStatsInputs.create(senseColorOptional, calibrationOptional, aggStatDeviceDataList, pillDataList);
            final Optional<AggStats> aggStats = AggStatsComputer.computeAggStats(accountId, deviceId, targetDateLocal, aggStatsInputs);
            if (!aggStats.isPresent()) {
                continue;
            }

            //Save aggregate statistics
            final Boolean successInsert = aggStatsProcessor.saveAggStat(aggStats.get());
            LOGGER.trace("action=insert-agg-stats success={} account_id={} overwrite={}", successInsert, aggStats.get().accountId, overwrite);
            numSuccess += 1;

        }

        return numSuccess;
    }

    private static ImmutableList<AggStatDeviceData> extractDeviceDataList(final ImmutableList<AggStatDeviceData> allAggStatDeviceData, final DateTime startLocalTime, final DateTime endLocalTime) {
        final List<AggStatDeviceData> outputAggStatDeviceDatas = Lists.newArrayList();

        for (final AggStatDeviceData aggStatDeviceData : allAggStatDeviceData) {
            if ( aggStatDeviceData.localTime.isAfter(startLocalTime.minus(1)) && aggStatDeviceData.localTime.isBefore(endLocalTime.plus(1)) ) {
                outputAggStatDeviceDatas.add(aggStatDeviceData);
            }
        }

        return ImmutableList.copyOf(outputAggStatDeviceDatas);
    }

    private static ImmutableList<AggStatTrackerMotion> extractTrackerMotionList(final ImmutableList<AggStatTrackerMotion> allAggStatTrackerMotion, final DateTime startLocalTime, final DateTime endLocalTime) {
        final List<AggStatTrackerMotion> outputAggStatTrackerMotions = Lists.newArrayList();

        for (final AggStatTrackerMotion aggStatTrackerMotion : allAggStatTrackerMotion) {
            if ( aggStatTrackerMotion.localTime.isAfter(startLocalTime.minus(1)) && aggStatTrackerMotion.localTime.isBefore(endLocalTime.plus(1))) {
                outputAggStatTrackerMotions.add(aggStatTrackerMotion);
            }
        }

        return ImmutableList.copyOf(outputAggStatTrackerMotions);
    }

}
