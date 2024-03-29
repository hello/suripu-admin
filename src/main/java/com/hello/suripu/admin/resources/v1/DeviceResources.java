package com.hello.suripu.admin.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.db.DeviceAdminDAO;
import com.hello.suripu.admin.db.DeviceAdminDAOImpl;
import com.hello.suripu.admin.models.DeviceAccountPairWrapper;
import com.hello.suripu.admin.models.DeviceAdmin;
import com.hello.suripu.admin.models.DeviceStatusBreakdown;
import com.hello.suripu.admin.models.InactiveDevicesPaginator;
import com.hello.suripu.core.configuration.ActiveDevicesTrackerConfiguration;
import com.hello.suripu.core.configuration.BlackListDevicesConfiguration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.PillViewsDynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB.ResponseCommand;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceInactivePage;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PillRegistration;
import com.hello.suripu.core.models.ProvisionRequest;
import com.hello.suripu.core.models.SenseRegistration;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.PillColorUtil;
import com.hello.suripu.core.util.SenseLogLevelUtil;
import com.hello.suripu.core.util.SerialNumberUtils;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisDataException;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Path("/v1/devices")
public class DeviceResources {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

  private final DeviceDAO deviceDAO;
  private final DeviceAdminDAO deviceAdminDAO;
  private final PillDataDAODynamoDB pillDataDAODynamoDB;
  private final AccountDAO accountDAO;
  private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
  private final KeyStore senseKeyStore;
  private final KeyStore pillKeyStore;
  private final JedisPool jedisPool;
  private final PillHeartBeatDAO pillHeartBeatDAO;
  private final SenseColorDAO senseColorDAO;
  private final ResponseCommandsDAODynamoDB responseCommandsDAODynamoDB;
  private final PillViewsDynamoDB pillViewsDynamoDB;
  private final SensorsViewsDynamoDB sensorsViewsDynamoDB;


  public DeviceResources(final DeviceDAO deviceDAO,
                         final DeviceAdminDAO deviceAdminDAO,
                         final PillDataDAODynamoDB pillDataDAODynamoDB,
                         final AccountDAO accountDAO,
                         final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                         final KeyStore senseKeyStore,
                         final KeyStore pillKeyStore,
                         final JedisPool jedisPool,
                         final PillHeartBeatDAO pillHeartBeatDAO,
                         final SenseColorDAO senseColorDAO,
                         final ResponseCommandsDAODynamoDB responseCommandsDAODynamoDB,
                         final PillViewsDynamoDB pillViewsDynamoDB,
                         final SensorsViewsDynamoDB sensorsViewsDynamoDB) {

    this.deviceDAO = deviceDAO;
    this.deviceAdminDAO = deviceAdminDAO;
    this.accountDAO = accountDAO;
    this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
    this.senseKeyStore = senseKeyStore;
    this.pillKeyStore = pillKeyStore;
    this.pillDataDAODynamoDB = pillDataDAODynamoDB;
    this.jedisPool = jedisPool;
    this.pillHeartBeatDAO = pillHeartBeatDAO;
    this.senseColorDAO = senseColorDAO;
    this.responseCommandsDAODynamoDB = responseCommandsDAODynamoDB;
    this.pillViewsDynamoDB = pillViewsDynamoDB;
    this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/sense")
  public List<DeviceAdmin> getSensesByEmail(@Auth final AccessToken accessToken,
                                            @QueryParam("email") final String email) {
    LOGGER.debug("Querying all senses for email = {}", email);
    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
    if (!accountIdOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity("Account not found!").build());
    }
    return getSensesByAccountId(accountIdOptional.get());
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/pill")
  public List<DeviceAdmin> getPillsByEmail(@Auth final AccessToken accessToken,
                                           @QueryParam("email") final String email) {
    LOGGER.debug("Querying all pills for email = {}", email);
    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
    if (!accountIdOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity("Account not found!").build());
    }
    return getPillsByAccountId(accountIdOptional.get());
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Path("/pill_status")
  @Produces(MediaType.APPLICATION_JSON)
  public List<DeviceStatus> getPillStatus(@Auth final AccessToken accessToken,
                                          @QueryParam("email") final String email,
                                          @QueryParam("pill_id_partial") final String pillIdPartial,
                                          @QueryParam("end_ts") final Long endTs,
                                          @QueryParam("limit") final Integer limitRaw) {

    final List<DeviceAccountPair> pills = new ArrayList<>();
    if (email == null && pillIdPartial == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity("Missing query params!").build());
    } else if (email != null) {
      LOGGER.debug("Querying all pills for email = {}", email);
      final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
      if (!accountIdOptional.isPresent()) {
        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
            .entity("Account not found!").build());
      }
      pills.addAll(deviceDAO.getPillsForAccountId(accountIdOptional.get()));
    } else {
      LOGGER.debug("Querying all pills whose IDs contain = {}", pillIdPartial);
      pills.addAll(deviceAdminDAO.getPillsByPillIdHint(pillIdPartial));
    }

    final Integer limit = limitRaw == null ? DeviceAdminDAOImpl.DEFAULT_PILL_STATUS_LIMIT : limitRaw;
    final List<DeviceStatus> pillStatuses = new ArrayList<>();
    for (final DeviceAccountPair pill : pills) {
      pillStatuses.addAll(deviceAdminDAO.pillStatusBeforeTs(pill.internalDeviceId, new DateTime(endTs, DateTimeZone.UTC), limit));
    }

    return pillStatuses;
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Path("/pill_heartbeat/{pill_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public DeviceStatus getPillHeartBeat(@Auth final AccessToken accessToken,
                                       @PathParam("pill_id") final String pillId) {

    final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getInternalPillId(pillId);
    if (!deviceAccountPairOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity("No pill found!").build());
    }


    final Optional<DeviceStatus> deviceStatusOptional = pillViewsDynamoDB.lastHeartBeat(deviceAccountPairOptional.get().externalDeviceId, deviceAccountPairOptional.get().internalDeviceId);
    if (deviceStatusOptional.isPresent()) {
      return deviceStatusOptional.get();
    }


    throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
        .entity("No heartbeat found!").build());
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @Timed
  @GET
  @Path("/{device_id}/accounts")
  @Produces(MediaType.APPLICATION_JSON)
  public ImmutableList<Account> getAccountsByDeviceIDs(@Auth final AccessToken accessToken,
                                                       @QueryParam("max_devices") final Long maxDevices,
                                                       @PathParam("device_id") final String deviceId) {
    final List<Account> accounts = new ArrayList<>();
    LOGGER.debug("Searching accounts who have used device {}", deviceId);
    accounts.addAll(deviceAdminDAO.getAccountsBySenseId(deviceId, maxDevices));
    if (accounts.isEmpty()) {
      accounts.addAll(deviceAdminDAO.getAccountsByPillId(deviceId, maxDevices));
    }
    return ImmutableList.copyOf(accounts);
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @Timed
  @GET
  @Path("/status_breakdown")
  @Produces(MediaType.APPLICATION_JSON)
  public DeviceStatusBreakdown getDeviceStatusBreakdown(@Auth final AccessToken accessToken,
                                                        @QueryParam("start_ts") final Long startTs,
                                                        @QueryParam("end_ts") final Long endTs) {
    // TODO: move this out of url handler once we've validated this is what we want

    if (startTs == null || endTs == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity("Require start_ts & end_ts").build());
    }

    final Jedis jedis = jedisPool.getResource();
    Long sensesCount = -1L;
    Long pillsCount = -1L;

    try {
      sensesCount = jedis.zcount(ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY, startTs, endTs);
      pillsCount = jedis.zcount(ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY, startTs, endTs);
    } catch (Exception e) {
      LOGGER.error("Failed to get active senses count because {}", e.getMessage());
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(e.getMessage()).build());
    } finally {
      jedisPool.returnResource(jedis);
    }

    LOGGER.debug("Senses count is {} from {} to {}", sensesCount, startTs, endTs);
    LOGGER.debug("Pills count is {} from {} to {}", pillsCount, startTs, endTs);

    return new DeviceStatusBreakdown(sensesCount, pillsCount);
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @Timed
  @GET
  @Path("/totals")
  @Produces(MediaType.APPLICATION_JSON)
  public DeviceStatusBreakdown getTotalDeviceStatusBreakdown(@Auth final AccessToken accessToken) {
    final Long sensesCount = deviceAdminDAO.getAllSensesCount();
    final Long pillsCount = deviceAdminDAO.getAllPillsCount();

    return new DeviceStatusBreakdown(sensesCount, pillsCount);
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Path("/inactive/sense")
  @Produces(MediaType.APPLICATION_JSON)
  public DeviceInactivePage getInactiveSenses(@Auth final AccessToken accessToken,
                                              @QueryParam("after") final Long afterTimestamp,
                                              @QueryParam("before") final Long beforeTimestamp,
                                              @QueryParam("limit") final Integer limit) {

    return new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY, limit)
        .generatePage();
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Path("/inactive/pill")
  @Produces(MediaType.APPLICATION_JSON)
  public DeviceInactivePage getInactivePills(@Auth final AccessToken accessToken,
                                             @QueryParam("after") final Long afterTimestamp,
                                             @QueryParam("before") final Long beforeTimestamp,
                                             @QueryParam("limit") final Integer limit) {

    InactiveDevicesPaginator inactiveDevicesPaginator;
    if (limit == null) {
      inactiveDevicesPaginator = new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY);
    } else {
      inactiveDevicesPaginator = new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY, limit);
    }
    return inactiveDevicesPaginator.generatePage();
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @POST
  @Path("/register/sense")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void registerSense(@Auth final AccessToken accessToken,
                            @Valid final SenseRegistration senseRegistration) {

    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, senseRegistration.email);
    if (!accountIdOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404, String.format("Account %s not found", senseRegistration.email))).build());
    }
    final Long accountId = accountIdOptional.get();


    try {
      final Long senseInternalId = deviceDAO.registerSense(accountId, senseRegistration.senseId);
      LOGGER.info("Account {} registered sense {} with internal id = {}", accountId, senseRegistration.senseId, senseInternalId);
    } catch (UnableToExecuteStatementException exception) {
      final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
      if (matcher.find()) {
        LOGGER.error("Failed to register sense for account id = {} and sense id = {} : {}", accountId, senseRegistration.senseId, exception.getMessage());
        throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
            .entity(new JsonError(409, "Sense already exists for this account.")).build());
      }
    }

    final List<DeviceAccountPair> deviceAccountMap = this.deviceDAO.getSensesForAccountId(accountId);

    for (final DeviceAccountPair deviceAccountPair : deviceAccountMap) {
      try {
        this.mergedUserInfoDynamoDB.setTimeZone(deviceAccountPair.externalDeviceId, accountId, DateTimeZone.forID(senseRegistration.timezone));
      } catch (AmazonServiceException awsException) {
        LOGGER.error("Aws failed when account {} tries to set timezone.", accountId);
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new JsonError(500, "Failed to set timezone")).build());
      } catch (IllegalArgumentException illegalArgumentException) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
            .entity(new JsonError(400, "Unrecognized timezone")).build());
      }
    }
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @POST
  @Path("/register/pill")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void registerPill(@Auth final AccessToken accessToken,
                           @Valid final PillRegistration pillRegistration) {

    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, pillRegistration.email);
    if (!accountIdOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404, String.format("Account %s not found", pillRegistration.email))).build());
    }
    final Long accountId = accountIdOptional.get();

    try {
      final Long trackerId = deviceDAO.registerPill(accountId, pillRegistration.pillId);
      LOGGER.info("Account {} registered pill {} with internal id = {}", accountId, pillRegistration.pillId, trackerId);

      final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
      if (sensePairedWithAccount.isEmpty()) {
        LOGGER.error("No sense paired with account {}", accountId);
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
            .entity(new JsonError(400, String.format("Registered pill %s but no sense has been paired to account %s", pillRegistration.pillId, pillRegistration.email))).build());
      }

      final String senseId = sensePairedWithAccount.get(0).externalDeviceId;
      this.mergedUserInfoDynamoDB.setNextPillColor(senseId, accountId, pillRegistration.pillId);

      final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getInternalPillId(pillRegistration.pillId);

      if (!deviceAccountPairOptional.isPresent()) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
            .entity(new JsonError(400, String.format("Pill %s not found!", pillRegistration.pillId))).build());
      }

      this.pillHeartBeatDAO.insert(deviceAccountPairOptional.get().internalDeviceId, 99, 0, 0, DateTime.now(DateTimeZone.UTC));

      return;
    } catch (UnableToExecuteStatementException exception) {
      final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());

      if (matcher.find()) {
        LOGGER.error("Failed to register pill for account id = {} and pill id = {} : {}", accountId, pillRegistration.pillId, exception.getMessage());
        throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
            .entity(new JsonError(409, "Pill already exists for this account.")).build());
      }
    } catch (AmazonServiceException awsEx) {
      LOGGER.error("Set pill color failed for pill {}, error: {}", pillRegistration.pillId, awsEx.getMessage());
    }

    throw new WebApplicationException(Response.Status.BAD_REQUEST);
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @DELETE
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/sense/{email}/{sense_id}")
  public void unregisterSenseByUser(@Auth final AccessToken accessToken,
                                    @PathParam("email") final String email,
                                    @PathParam("sense_id") final String senseId,
                                    @QueryParam("unlink_all") @DefaultValue("false") final Boolean unlinkAll) {

    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
    if (!accountIdOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404, String.format("Account %s not found", email))).build());
    }
    final Long accountId = accountIdOptional.get();
    final List<UserInfo> pairedUsers = mergedUserInfoDynamoDB.getInfo(senseId);

    if (pairedUsers.isEmpty()) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(new JsonError(400, String.format("Sense %s has not been paired to any account", senseId))).build());
    }

    final List<Long> pairedAccountIdList = new ArrayList<>();
    for (final UserInfo pairUser : pairedUsers) {
      pairedAccountIdList.add(pairUser.accountId);
    }

    if (!pairedAccountIdList.contains(accountId)) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(new JsonError(400, String.format("Sense %s has not been paired to %s", senseId, email))).build());
    }


    //            this.deviceDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, DeviceDAO>() {
    //                @Override
    //                public Void inTransaction(final DeviceDAO transactional, final TransactionStatus status) throws Exception {
    //                    final Integer pillDeleted = transactional.deletePillPairingByAccount(accountId);
    //                    LOGGER.info("Factory reset delete {} Pills linked to account {}", pillDeleted, accountId);
    //
    //                    final Integer accountUnlinked = transactional.unlinkAllAccountsPairedToSense(senseId);
    //                    LOGGER.info("Factory reset delete {} accounts linked to Sense {}", accountUnlinked, accountId);
    //
    //                    try {
    //                        mergedUserInfoDynamoDB.unlinkAccountToDevice(accountId, senseId);
    //                    } catch (AmazonServiceException awsEx) {
    //                        LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
    //                                accountId,
    //                                senseId,
    //                                awsEx.getErrorMessage());
    //                    }
    //
    //                    return null;
    //                }
    //            });


    if (unlinkAll.equals(Boolean.TRUE)) {
      deviceDAO.unlinkAllAccountsPairedToSense(senseId);
    } else {
      deviceDAO.deleteSensePairing(senseId, accountId);
    }

    try {
      mergedUserInfoDynamoDB.unlinkAccountToDevice(accountId, senseId);
    } catch (AmazonServiceException awsEx) {
      LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
          accountId,
          senseId,
          awsEx.getErrorMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (UnableToExecuteStatementException sqlExp) {
      LOGGER.error("Failed to factory reset Sense {}, error {}", senseId, sqlExp.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @DELETE
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/pill/{email}/{pill_id}")
  public void unregisterPill(@Auth final AccessToken accessToken,
                             @PathParam("email") final String email,
                             @PathParam("pill_id") String externalPillId) {

    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
    if (!accountIdOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404, String.format("Account %s not found", email))).build());
    }
    final Long accountId = accountIdOptional.get();

    final Integer numRows = deviceDAO.deletePillPairing(externalPillId, accountId);
    if (numRows == 0) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404, String.format("Did not find active pill %s to unregister for %s", externalPillId, email))).build());
    }

    final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
    if (sensePairedWithAccount.size() == 0) {
      LOGGER.error("No sense paired with account {}", accountId);
      return;
    }

    final String senseId = sensePairedWithAccount.get(0).externalDeviceId;

    try {
      this.mergedUserInfoDynamoDB.deletePillColor(senseId, accountId, externalPillId);
    } catch (Exception ex) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404,
              String.format("Failed to delete pill %s color from user info table for sense %s and account %s because %s",
                  externalPillId, senseId, accountId, ex.getMessage()))).build());
    }
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @POST
  @Path("/provision/sense")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void senseProvision(@Auth final AccessToken accessToken, @Valid final ProvisionRequest provisionRequest) {
    senseKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @POST
  @Path("/provision/pill")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void pillProvision(@Auth final AccessToken accessToken, @Valid final ProvisionRequest provisionRequest) {
    pillKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @POST
  @Path("/provision/batch_pills")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void batchPillsProvision(@Auth final AccessToken accessToken, @Valid final List<ProvisionRequest> provisionRequests) {
    for (final ProvisionRequest provisionRequest : provisionRequests) {
      pillKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
    }
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Path("/timezone")
  @Produces(MediaType.APPLICATION_JSON)
  public TimeZoneHistory getTimezone(@Auth final AccessToken accessToken,
                                     @QueryParam("sense_id") final String senseId,
                                     @QueryParam("email") final String email,
                                     @QueryParam("event_ts") final Long eventTs) {

    if (senseId == null && email == null) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Require sense_id OR email!").build());
    }
    final DateTime eventDateTime = eventTs == null ? DateTime.now(DateTimeZone.UTC) : new DateTime(eventTs);

    final Optional<TimeZoneHistory> timeZoneHistoryOptional = (senseId != null) ?
        getTimeZoneBySenseId(senseId, eventDateTime) : getTimeZoneByEmail(email, eventDateTime);

    if (!timeZoneHistoryOptional.isPresent()) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
          .entity(new JsonError(404, "Failed to retrieve timezone")).build());
    }
    return timeZoneHistoryOptional.get();
  }


  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Path("/color/missing")
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> missingColors(@Auth final AccessToken accessToken) {
    final List<String> deviceIdsMissingColor = senseColorDAO.missing();
    return deviceIdsMissingColor;
  }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/color/{sense_id}")
    public String getColor(@Auth final AccessToken accessToken,
                         @PathParam("sense_id") final String senseId) {
      final Optional<DeviceKeyStoreRecord> recordFromDDB = senseKeyStore.getKeyStoreRecord(senseId);
      if(!recordFromDDB.isPresent()) {
        LOGGER.warn("action=get-color sense_id={} msg=missing-keystore-record");
        return "UNKNOWN";
      }
      final DeviceKeyStoreRecord record = recordFromDDB.get();
      final Optional<Sense.Color> colorOptional = SerialNumberUtils.extractColorFrom(record.metadata);
      if(colorOptional.isPresent()) {
          return colorOptional.get().name();
      }
      LOGGER.warn("action=get-color sense_id={} invalid_sn={}", record.metadata);
      return "UNKOWN";
    }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Path("/pill_color/{sense_id}/{account_id}")
  public String getColor(@Auth final AccessToken accessToken,
                         @NotNull @PathParam("sense_id") final String senseId,
                         @NotNull @PathParam("account_id") final Long accountId) {

    final Optional<UserInfo> userInfoOptional = mergedUserInfoDynamoDB.getInfo(senseId, accountId);
    final String defaultColorName = Device.Color.valueOf("BLUE").name();

    if (!userInfoOptional.isPresent()) {
      return defaultColorName;
    }

    final UserInfo userInfo = userInfoOptional.get();

    if (userInfo.pillColor.isPresent()) {
      final Device.Color pillColor = PillColorUtil.displayDeviceColor(userInfo.pillColor.get().getPillColor());
      return pillColor.name();
    }

    return defaultColorName;
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @PUT
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/sense_black_list")
  public Response updateSenseBlackList(@Auth final AccessToken accessToken,
                                       @Valid final Set<String> updatedSenseBlackList) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      final Pipeline pipe = jedis.pipelined();
      pipe.multi();
      pipe.del(BlackListDevicesConfiguration.SENSE_BLACK_LIST_KEY);
      for (final String senseId : updatedSenseBlackList) {
        pipe.sadd(BlackListDevicesConfiguration.SENSE_BLACK_LIST_KEY, senseId);
      }
      pipe.exec();
    } catch (JedisDataException e) {
      if (jedis != null) {
        jedisPool.returnBrokenResource(jedis);
        jedis = null;
      }
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              String.format("Failed to get data from redis - %s", e.getMessage()))).build());
    } catch (Exception e) {
      if (jedis != null) {
        jedisPool.returnBrokenResource(jedis);
        jedis = null;
      }
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              String.format("Failed to update sense black list because %s", e.getMessage()))).build());
    } finally {
      if (jedis != null) {
        jedisPool.returnResource(jedis);
      }
    }
    return Response.noContent().build();
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/sense_black_list")
  public Set<String> addSenseBlackList(@Auth final AccessToken accessToken) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      return jedis.smembers(BlackListDevicesConfiguration.SENSE_BLACK_LIST_KEY);
    } catch (JedisDataException e) {
      if (jedis != null) {
        jedisPool.returnBrokenResource(jedis);
        jedis = null;
      }
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              String.format("Failed to get data from redis - %s", e.getMessage()))).build());
    } catch (Exception e) {
      if (jedis != null) {
        jedisPool.returnBrokenResource(jedis);
        jedis = null;
      }
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              String.format("Failed to retrieve sense black list because %s", e.getMessage()))).build());
    } finally {
      if (jedis != null) {
        jedisPool.returnResource(jedis);
      }
    }
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Path("/invalid/sense")
  @Produces(MediaType.APPLICATION_JSON)
  public Set<String> getInvalidActiveSenses(@Auth final AccessToken accessToken,
                                            @QueryParam("start_ts") final Long startTs,
                                            @QueryParam("end_ts") final Long endTs) {

    if (startTs == null || endTs == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity("Require start_ts & end_ts").build());
    }

    final Jedis jedis = jedisPool.getResource();
    final Set<String> allSeenSenses = new HashSet<>();
    final Set<String> allValidSeenSenses = new HashSet<>();

    try {
      final Pipeline pipe = jedis.pipelined();
      final redis.clients.jedis.Response<Set<String>> allDevs = pipe.zrangeByScore(ActiveDevicesTrackerConfiguration.ALL_DEVICES_SEEN_SET_KEY, startTs, endTs);
      final redis.clients.jedis.Response<Set<String>> allValid = pipe.zrangeByScore(ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY, startTs, endTs);
      pipe.sync();

      allSeenSenses.addAll(allDevs.get());
      allValidSeenSenses.addAll(allValid.get());
    } catch (Exception e) {
      LOGGER.error("Failed retrieving invalid active senses.", e.getMessage());
    } finally {
      jedisPool.returnResource(jedis);
    }

    //Returns all the devices that did not get paired account info or had no timezone in the sense save worker
    allSeenSenses.removeAll(allValidSeenSenses);
    return allSeenSenses;
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/update_timezone_by_partner/{account_id}")
  public Response updateTimeZoneByPartner(@Auth final AccessToken accessToken,
                                          @NotNull @PathParam("account_id") final Long accountId) {

    final Optional<Long> partnerAccountIdOptional = deviceDAO.getPartnerAccountId(accountId);
    if (!partnerAccountIdOptional.isPresent()) {
      LOGGER.warn("Cannot update timezone by partner for {} - Partner not found", accountId);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Partner not found").build());
    }

    final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
    if (!deviceAccountPairOptional.isPresent()) {
      LOGGER.warn("Cannot update timezone by partner for {} - No sense paired to this account", accountId);
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("No sense paired to this account").build());
    }
    final Optional<DateTimeZone> timeZoneOptional = mergedUserInfoDynamoDB.getTimezone(deviceAccountPairOptional.get().externalDeviceId, accountId);

    if (timeZoneOptional.isPresent()) {
      LOGGER.warn("Cannot update timezone by partner for {} - This account already got a timezone", accountId);
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This account already got a timezone").build());
    }

    final Optional<DeviceAccountPair> partnerdeviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(partnerAccountIdOptional.get());
    if (!partnerdeviceAccountPairOptional.isPresent()) {
      LOGGER.warn("Cannot update timezone by partner for {} - Partner does not have a sense", accountId);
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("No sense paired to partner account").build());
    }
    final Optional<DateTimeZone> partnerTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(partnerdeviceAccountPairOptional.get().externalDeviceId, partnerAccountIdOptional.get());

    if (!partnerAccountIdOptional.isPresent()) {
      LOGGER.warn("Cannot update timezone by partner for {} - Partner does not have a timezone", accountId);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Partner does not have a timezone").build());
    }

    try {
      mergedUserInfoDynamoDB.setTimeZone(deviceAccountPairOptional.get().externalDeviceId, accountId, partnerTimeZoneOptional.get());
    } catch (AmazonServiceException awsException) {
      LOGGER.error("Failed to set timezone for account {} by partner {} because {}", accountId, partnerAccountIdOptional.get(), awsException.getMessage());
      throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
    }

    return Response.noContent().build();
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @PUT
  @Timed
  @Path("/{device_id}/reset_mcu")
  public void resetDeviceMCU(@Auth final AccessToken accessToken,
                             @PathParam("device_id") final String deviceId,
                             @QueryParam("fw_version") final Integer fwVersion) {

    if (deviceId == null || fwVersion == null) {
      LOGGER.error("One of the following parameters is missing. device_id: {}, fwVersion: {}", deviceId, fwVersion);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    LOGGER.info("Resetting device: {} on FW Version: {}", deviceId, fwVersion);

    final Map<ResponseCommand, String> issuedCommands = new ImmutableMap.Builder<ResponseCommand, String>()
        .put(ResponseCommand.RESET_MCU, "true")
        .build();

    responseCommandsDAODynamoDB.insertResponseCommands(deviceId, fwVersion, issuedCommands);
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
  @PUT
  @Timed
  @Path("/{device_id}/set_log_level")
  public void setLogLevel(@Auth final AccessToken accessToken,
                          @PathParam("device_id") final String deviceId,
                          @QueryParam("fw_version") final Integer fwVersion,
                          @QueryParam("log_level") final String logLevel) {

    if (deviceId == null || fwVersion == null || logLevel == null) {
      LOGGER.error("One of the following parameters is missing. device_id: {}, fwVersion: {}, logLevel: {}", deviceId, fwVersion, logLevel);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    try {
      final SenseLogLevelUtil.LogLevel senseLogLevel = SenseLogLevelUtil.getLogLevelFromString(logLevel);
      LOGGER.info("Setting log level for device: {} to '{}'", deviceId, senseLogLevel.toString());

      final Map<ResponseCommand, String> issuedCommands = new ImmutableMap.Builder<ResponseCommand, String>()
          .put(ResponseCommand.SET_LOG_LEVEL, senseLogLevel.value.toString())
          .build();

      responseCommandsDAODynamoDB.insertResponseCommands(deviceId, fwVersion, issuedCommands);

    } catch (Exception ex) {
      LOGGER.error("Failed Setting Log Level: {}", ex.getMessage());
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/get_log_levels")
  public Set<String> getLogLevels(@Auth final AccessToken accessToken) {

    final List<SenseLogLevelUtil.LogLevel> logLevels = Lists.newArrayList(SenseLogLevelUtil.LogLevel.values());
    final Set<String> logLevelNames = Sets.newHashSet();

    for (final SenseLogLevelUtil.LogLevel level : logLevels) {
      logLevelNames.add(level.name());
    }

    return logLevelNames;
  }

  @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/recent")
  public List<DeviceAccountPairWrapper> recent(@Auth AccessToken accessToken) {
    final List<DeviceAccountPair> pairs = deviceAdminDAO.getMostRecentPairs(90, Integer.MAX_VALUE);
    final Set<String> uniqueDeviceIds = pairs.stream()
            .map(p -> p.externalDeviceId)
            .collect(Collectors.toSet());

    final Map<String, DeviceKeyStoreRecord> records = senseKeyStore.getKeyStoreRecordBatch(uniqueDeviceIds);
    final List<DeviceAccountPairWrapper> pairWrappers = new ArrayList<>();
    for(final DeviceAccountPair pair : pairs) {
        if(records.containsKey(pair.externalDeviceId)) {
          pairWrappers.add(new DeviceAccountPairWrapper(pair, records.get(pair.externalDeviceId)));
        }
    }

    return pairWrappers;
  }



  // Helpers
  private List<DeviceAdmin> getSensesByAccountId(final Long accountId) {
    final ImmutableList<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(accountId);
    final List<DeviceAdmin> senses = new ArrayList<>();

    for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
      final Optional<DeviceData> deviceDataOptional = sensorsViewsDynamoDB.lastSeen(senseAccountPair.externalDeviceId, accountId, senseAccountPair.internalDeviceId);
      if (!deviceDataOptional.isPresent()) {
        senses.add(DeviceAdmin.create(senseAccountPair));
      } else {
        final DeviceData deviceData = deviceDataOptional.get();
        LOGGER.debug("device data {}", deviceData);
        senses.add(DeviceAdmin.create(senseAccountPair, DeviceStatus.sense(deviceData.deviceId, Integer.toHexString(deviceData.firmwareVersion), deviceData.dateTimeUTC)));
      }
    }
    return senses;
  }

  private List<DeviceAdmin> getPillsByAccountId(final Long accountId) {
    final ImmutableList<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(accountId);
    final List<DeviceAdmin> pills = new ArrayList<>();

    for (final DeviceAccountPair pillAccountPair : pillAccountPairs) {
      Optional<DeviceStatus> pillStatusOptional = this.pillHeartBeatDAO.getPillStatus(pillAccountPair.internalDeviceId);

      if (!pillStatusOptional.isPresent()) {
        LOGGER.warn("Failed to get heartbeat for account id {} on pill internal id: {} - external id: {}, looking into tracker motion", accountId, pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId);
        final Optional<TrackerMotion> latest = this.pillDataDAODynamoDB.getMostRecent(pillAccountPair.externalDeviceId, accountId, DateTime.now());
        if (latest.isPresent()) {
          final TrackerMotion pillData = latest.get();
          final DeviceStatus deviceStatus = new DeviceStatus(0L, 0L, "1", 100, new DateTime(pillData.timestamp, DateTimeZone.UTC), 0);
          pillStatusOptional = Optional.of (deviceStatus);
        }
      }

      if (!pillStatusOptional.isPresent()) {
        pills.add(DeviceAdmin.create(pillAccountPair));
      } else {
        pills.add(DeviceAdmin.create(pillAccountPair, pillStatusOptional.get()));
      }
    }

    return pills;
  }

  private Optional<TimeZoneHistory> getTimeZoneBySenseId(final String senseId, final DateTime eventDateTime) {
    final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseId);
    if (userInfoList.isEmpty()) {
      return Optional.absent();
    }
    final Optional<DateTimeZone> dateTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(senseId, userInfoList.get(0).accountId);

    if (!dateTimeZoneOptional.isPresent()) {
      return Optional.absent();
    }
    return Optional.of(new TimeZoneHistory(dateTimeZoneOptional.get().getOffset(eventDateTime), dateTimeZoneOptional.get().getID()));
  }

  private Optional<TimeZoneHistory> getTimeZoneByEmail(final String email, final DateTime eventDateTime) {
    final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
    if (!accountIdOptional.isPresent()) {
      return Optional.absent();
    }

    final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountIdOptional.get());
    if (!deviceAccountPairOptional.isPresent()) {
      return Optional.absent();
    }
    final Optional<DateTimeZone> dateTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(deviceAccountPairOptional.get().externalDeviceId, accountIdOptional.get());

    if (!dateTimeZoneOptional.isPresent()) {
      return Optional.absent();
    }
    return Optional.of(new TimeZoneHistory(dateTimeZoneOptional.get().getOffset(eventDateTime), dateTimeZoneOptional.get().getID()));
  }
}
