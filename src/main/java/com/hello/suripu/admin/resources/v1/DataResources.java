package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hello.suripu.admin.Util;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.logging.SenseLogTag;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.roomstate.CurrentRoomState;
import com.hello.suripu.core.models.DataScience.UserLabel;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInteraction;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.SmoothSample;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;


@Path("/v1/data")
public class DataResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataResources.class);
    private static final Boolean USE_AUDIO_PEAK_ENERGY_DB = true; // always show new audio peak energy in admin tool
    private static final Float NO_SOUND_FILL_VALUE_DB = (float) 25; // Replace with 25 decibels when Sense isn't capturing audio

    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final DeviceDAO deviceDAO;
    private final AccountDAO accountDAO;
    private final UserLabelDAO userLabelDAO;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final SenseColorDAO senseColorDAO;
    private final CalibrationDAO calibrationDAO;
    private final PillDataDAODynamoDB pillDataDAODynamoDB;

    public DataResources(final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                         final DeviceDAO deviceDAO,
                         final AccountDAO accountDAO,
                         final UserLabelDAO userLabelDAO,
                         final SensorsViewsDynamoDB sensorsViewsDynamoDB,
                         final SenseColorDAO senseColorDAO,
                         final CalibrationDAO calibrationDAO,
                         final PillDataDAODynamoDB pillDataDAODynamoDB) {

        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
        this.userLabelDAO = userLabelDAO;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.senseColorDAO = senseColorDAO;
        this.calibrationDAO = calibrationDAO;
        this.pillDataDAODynamoDB = pillDataDAODynamoDB;
    }

    @ScopesAllowed({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH})
    @GET
    @Path("/user_interaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserInteraction> getUserInteractions(@Auth final AccessToken accessToken,
                                                     @QueryParam("email") String email,
                                                     @QueryParam("account_id") Long accountId,
                                                     @QueryParam("start_ts") Long startTimestamp,
                                                     @QueryParam("end_ts") Long endTimestamp,
                                                     @QueryParam("with_calibrated_dust") @DefaultValue("true") final Boolean withCalibratedDust) {

        if ( (email == null && accountId == null) || (startTimestamp == null || endTimestamp == null) ) {
            throw new WebApplicationException(Response.status(400).entity(new JsonError(400,
                "Missing query parameters, use email or account_id, and start_ts and end_ts")).build());
        }

        Optional<Account> optionalAccount;
        if (email != null) {
            optionalAccount = accountDAO.getByEmail(email);
        } else {
            optionalAccount = accountDAO.getById(accountId);
        }

        if (!optionalAccount.isPresent() || !optionalAccount.get().id.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Account account = optionalAccount.get();
        LOGGER.debug("Getting user interactions for account {} between {} and {}", account.id.get(), startTimestamp, endTimestamp);

        return getUserInteractionsData(account.id.get(), startTimestamp, endTimestamp, withCalibratedDust);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/pill/{email}/{query_date_local_utc}/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotionAdmin(@Auth final AccessToken accessToken,
                                              @PathParam("query_date_local_utc") String date,
                                              @PathParam("email") String email) {

        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> accountId = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<TrackerMotion> trackerMotions = pillDataDAODynamoDB.getBetweenLocalUTC(accountId.get(), targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/pill/last24/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotionAdminLast24(@Auth final AccessToken accessToken, @PathParam("email") String email){

        final DateTime endDate = DateTime.now(DateTimeZone.UTC);
        final DateTime startDate = endDate.minusDays(1);
        LOGGER.debug("Start date: {}", startDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> accountId = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<TrackerMotion> trackerMotions = pillDataDAODynamoDB.getBetween(accountId.get(), startDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }



    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Path("/{email}/{sensor}/{resolution}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sample> getAdminLastDay(
            @Auth AccessToken accessToken,
            @PathParam("email") final String email,
            @PathParam("sensor") final String sensorString,
            @PathParam("resolution") final String resolution,
            @QueryParam("from") Long queryEndTimestampInUTC,
            @QueryParam("with_calibrated_dust") @DefaultValue("true") final Boolean withCalibratedDust,
            @QueryParam("smooth") @DefaultValue("true") final Boolean smooth) {

        final Optional<Long> optionalAccountId = Util.getAccountIdByEmail(accountDAO, email);
        if (!optionalAccountId.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // get latest device_id connected to this account
        final Long accountId = optionalAccountId.get();
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if(!deviceIdPair.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final Optional<Device.Color> color = senseColorDAO.getColorForSense(deviceIdPair.get().externalDeviceId);


        int slotDurationInMinutes;
        int limitDays;

        switch (resolution) {
            case "week":
                slotDurationInMinutes = 60;
                limitDays = 7;
                break;
            case "day":
                slotDurationInMinutes = 5;
                limitDays = 1;
                break;
            case "minute":
                slotDurationInMinutes = 1;
                limitDays = 1;
                break;
            default:
                slotDurationInMinutes = 60;
                limitDays = 1;
        }

        /*
        * We have to minutes one day instead of 24 hours, for the same reason that we want one DAY's
        * data, instead of 24 hours.
         */
        final long queryStartTimeInUTC = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC).minusDays(limitDays).getMillis();
        final Sensor sensor = Sensor.fromString(sensorString);

        final List<Sample> timeSeries = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(queryStartTimeInUTC, queryEndTimestampInUTC,
                accountId, deviceIdPair.get().externalDeviceId, slotDurationInMinutes, sensor, 0, color, getCalibration(deviceIdPair.get().externalDeviceId,
                        withCalibratedDust), USE_AUDIO_PEAK_ENERGY_DB);

        if (Sensor.PARTICULATES.equals(sensor)) {
            if (smooth) {
                return SmoothSample.convert(timeSeries);
            }
        }
        return timeSeries;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Path("/label")
    @Consumes(MediaType.APPLICATION_JSON)
    public void label(@Auth final AccessToken accessToken,
                      @Valid final UserLabel label) {

        final Optional<Long> optionalAccountId = Util.getAccountIdByEmail(accountDAO, label.email);
        if (!optionalAccountId.isPresent()) {
            LOGGER.debug("Account {} not found", label.email);
            return;
        }

        UserLabel.UserLabelType userLabel = UserLabel.UserLabelType.fromString(label.labelString);

        final DateTime nightDate = DateTime.parse(label.night, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();

        final DateTime labelTimestampUTC = new DateTime(label.ts, DateTimeZone.UTC);

        userLabelDAO.insertUserLabel(optionalAccountId.get(),
                label.email, userLabel.toString().toLowerCase(),
                nightDate, labelTimestampUTC, label.durationMillis, labelTimestampUTC.plusMillis(label.tzOffsetMillis),
                label.tzOffsetMillis, label.note);

    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Path("/batch_label")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public int label(@Auth final AccessToken accessToken,
                     @Valid final List<UserLabel> labels) {

        final List<Long> accountIds = new ArrayList<>();
        final List<String> emails = new ArrayList<>();
        final List<String> userLabels = new ArrayList<>();
        final List<DateTime> nightDates = new ArrayList<>();
        final List<DateTime> UTCTimestamps = new ArrayList<>();
        final List<Integer> durations = new ArrayList<>();
        final List<DateTime> localUTCTimestamps = new ArrayList<>();
        final List<Integer> tzOffsets = new ArrayList<>();
        final List<String> notes = new ArrayList<>();

        for (UserLabel label : labels) {

            final Optional<Long> optionalAccountId = Util.getAccountIdByEmail(accountDAO, label.email);
            if (!optionalAccountId.isPresent()) {
                LOGGER.debug("Account {} not found", label.email);
                continue;
            }

            final Long accountId = optionalAccountId.get();
            accountIds.add(accountId);
            emails.add(label.email);

            UserLabel.UserLabelType userLabel = UserLabel.UserLabelType.fromString(label.labelString);
            userLabels.add(userLabel.toString().toLowerCase());

            final DateTime nightDate = DateTime.parse(label.night, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                    .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
            nightDates.add(nightDate);

            final DateTime labelTimestampUTC = new DateTime(label.ts, DateTimeZone.UTC);
            UTCTimestamps.add(labelTimestampUTC);
            durations.add(label.durationMillis);
            localUTCTimestamps.add(labelTimestampUTC.plusMillis(label.tzOffsetMillis));

            tzOffsets.add(label.tzOffsetMillis);

            notes.add(label.note);
        }

        int inserted = 0;
        try {
            userLabelDAO.batchInsertUserLabels(accountIds, emails, userLabels, nightDates,
                    UTCTimestamps, durations, localUTCTimestamps, tzOffsets, notes);
            inserted = accountIds.size();
        } catch (UnableToExecuteStatementException exception) {
            LOGGER.warn("Batch insert user labels fails for some reason");
        }

        return inserted;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Path("/label/{email}/{night}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserLabel> getLabels(@Auth final AccessToken accessToken,
                                     @PathParam("email") String email,
                                     @PathParam("night") String night) {
        final DateTime nightDate = DateTime.parse(night, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
        LOGGER.debug("{} {}", email, nightDate);
        return userLabelDAO.getUserLabelsByEmailAndNight(email, nightDate);
    }


    //Helpers
    private List<UserInteraction> getUserInteractionsData(final Long accountId, final Long startTimestamp, final Long endTimestamp, final Boolean withCalibratedDust) {

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("This account does not have a sense recently").build());
        }

        final Long timeRangeLimitMillis = 3 * 86400 * 1000L;
        if ( (endTimestamp - startTimestamp) >  timeRangeLimitMillis) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Maximum time range (3 days) exceeded").build());
        }

        final int slotDurationInMinutes = 5;
        final Integer missingDataDefaultValue = 0;

        final Optional<Device.Color> color = senseColorDAO.getColorForSense(deviceAccountPairOptional.get().externalDeviceId);

        final AllSensorSampleList sensorSamples = deviceDataDAODynamoDB.generateTimeSeriesByUTCTimeAllSensors(
                startTimestamp,
                endTimestamp,
                accountId,
                deviceAccountPairOptional.get().externalDeviceId,
                slotDurationInMinutes,
                missingDataDefaultValue,
                color,
                getCalibration(deviceAccountPairOptional.get().externalDeviceId, withCalibratedDust),
                USE_AUDIO_PEAK_ENERGY_DB
        );

        final List<UserInteraction> userInteractions = new ArrayList<>();

        LOGGER.info("{}", sensorSamples.getAvailableSensors());

        final List<Sample> waveCountData = sensorSamples.get(Sensor.WAVE_COUNT);
        final List<Sample> holdCountData = sensorSamples.get(Sensor.HOLD_COUNT);
        LOGGER.info("wave size {}", waveCountData.size());
        LOGGER.info("hold size {}", holdCountData.size());
        for (int i=0; i<waveCountData.size(); i++) {
            userInteractions.add(new UserInteraction(
                waveCountData.get(i).value,
                holdCountData.get(i).value,
                waveCountData.get(i).dateTime,
                waveCountData.get(i).offsetMillis
            ));
        }

        return userInteractions;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Path("/current_room_conditions/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public CurrentRoomState currentRoomState(@Auth AccessToken accessToken,
                                             @PathParam("sense_id") final String senseId,
                                             @QueryParam("with_calibrated_dust") @DefaultValue("true") final Boolean withCalibratedDust) {

        final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(senseId);
        if(pairs.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final DeviceAccountPair pair = pairs.get(0);

        final Optional<DeviceData> deviceDataOptional = sensorsViewsDynamoDB.lastSeen(senseId, pair.accountId, pair.internalDeviceId);
        if(!deviceDataOptional.isPresent()) {
            return CurrentRoomState.empty(withCalibratedDust);
        }
        return CurrentRoomState.fromDeviceData(deviceDataOptional.get().withCalibratedLight(senseColorDAO.getColorForSense(senseId)), DateTime.now(), 15, "c", getCalibration(senseId, withCalibratedDust), NO_SOUND_FILL_VALUE_DB).withDust(withCalibratedDust);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Path("/log_tags")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getSenseLogsTag (@Auth AccessToken accessToken,
                                   @PathParam("sense_id") final String senseId) {
        return SenseLogTag.rawValues();

    }

    private Optional<Calibration> getCalibration(final String senseId, final Boolean withCalibratedDust) {
        return withCalibratedDust ? calibrationDAO.getStrict(senseId) : Optional.<Calibration>absent();
    }
}
