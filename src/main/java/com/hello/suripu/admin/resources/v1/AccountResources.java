package com.hello.suripu.admin.resources.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.db.AccountAdminDAO;
import com.hello.suripu.admin.db.DeviceAdminDAO;
import com.hello.suripu.admin.db.DeviceAdminDAOImpl;
import com.hello.suripu.admin.models.PasswordResetAdmin;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountCount;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.SmartAlarmHistory;
import com.hello.suripu.core.models.TimeHistory;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.passwordreset.PasswordReset;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.profile.ImmutableProfilePhoto;
import com.hello.suripu.core.profile.ProfilePhotoStore;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.PasswordUtil;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.Auth;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/v1/account")
public class AccountResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountResources.class);
    private final Integer DEFAULT_RECENT_USERS_LIMIT = 100;
    private final Integer MAX_RECENT_USERS_LIMIT = 500;

    private final AccountDAO accountDAO;

    private final PasswordResetDB passwordResetDB;
    private final DeviceDAO deviceDAO;
    private final AccountAdminDAO accountAdminDAO;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB;
    private final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB;
    private final DeviceAdminDAO deviceAdminDAO;
    private final ProfilePhotoStore profilePhotoStore;

    @Context
    HttpServletRequest request;
    public AccountResources(final AccountDAO accountDAO,
                            final PasswordResetDB passwordResetDB,
                            final DeviceDAO deviceDAO,
                            final AccountAdminDAO accountAdminDAO,
                            final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
                            final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB,
                            final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                            final DeviceAdminDAO deviceAdminDAO,
                            final ProfilePhotoStore profilePhotoStore) {
        this.accountDAO = accountDAO;
        this.passwordResetDB = passwordResetDB;
        this.deviceDAO = deviceDAO;
        this.accountAdminDAO = accountAdminDAO;
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
        this.smartAlarmLoggerDynamoDB = smartAlarmLoggerDynamoDB;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;
        this.deviceAdminDAO = deviceAdminDAO;
        this.profilePhotoStore = profilePhotoStore;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Account retrieveAccountByEmailOrId(@Auth final AccessToken accessToken,
                                              @QueryParam("email") final String email,
                                              @QueryParam("id") final String id) {

        if (email == null &&  id == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Missing query params, please specify email or id").build());
        }

        if (email != null) {
            LOGGER.debug("Looking account up by email {}", email);
            final Optional<Account> accountByEmailOptional = accountDAO.getByEmail(email.toLowerCase());
            if (!accountByEmailOptional.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Account not found").build());
            }
            return withPhotoMaybe(accountByEmailOptional.get());
        } else {
            Optional<Account> optionalAccount;
            LOGGER.debug("Looking up account by id {}", id);
            try {
                final UUID externalId = UUID.fromString(id);
                optionalAccount = accountAdminDAO.getByExternalId(externalId);
            }  catch (IllegalArgumentException exception) {
                optionalAccount = accountDAO.getById(Long.parseLong(id));
            }

            if (!optionalAccount.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Account not found").build());
            }
            return withPhotoMaybe(optionalAccount.get());
        }
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/partial")
    public List<Account> retrieveAccountsByEmailPartial(@Auth final AccessToken accessToken,
                                                        @QueryParam("email") final String emailPartial,
                                                        @QueryParam("name") final String namePartial) {
        if (emailPartial != null) {
            LOGGER.debug("Looking up accounts whose emails contain {}", emailPartial);
            return accountDAO.getByEmailPartial(emailPartial);
        }

        if (namePartial != null) {
            LOGGER.debug("Looking up accounts whose names contain {}", namePartial);
            return accountDAO.getByNamePartial(namePartial);
        }

        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Missing email/name partials input").build());
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recent")
    public List<Account> retrieveRecentlyCreatedAccounts(@Auth final AccessToken accessToken,
                                                         @QueryParam("limit") final Integer limit){
        if (limit == null) {
            return accountDAO.getRecent(DEFAULT_RECENT_USERS_LIMIT);
        }
        return accountDAO.getRecent(Math.min(limit, MAX_RECENT_USERS_LIMIT));
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/paginate")
    public List<Account> retrievePaginatedAccounts(@Auth final AccessToken accessToken,
                                                         @QueryParam("limit") final Integer limit,
                                                         @QueryParam("max_id") final Integer maxId){
        if (limit == null || maxId == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "limit and max_id required")).build());
        }
        return accountAdminDAO.getRecentBeforeId(Math.min(limit, MAX_RECENT_USERS_LIMIT), maxId);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/update_password")
    public Response passwordUpdate(@Auth final AccessToken accessToken, final PasswordResetAdmin passwordResetAdmin) {

        LOGGER.debug("Admin {} attempts to set passsword for email {}", accessToken.accountId, passwordResetAdmin.email);
        final Optional<Account> accountOptional = accountDAO.getByEmail(passwordResetAdmin.email.toLowerCase());
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "account not found")).build());
        }

        if (passwordResetAdmin.password.length() < 6){
            throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity("Password length should be greater than 6").build());
        }

        final PasswordReset passwordReset = PasswordReset.create(accountOptional.get());
        passwordResetDB.save(passwordReset);



        final Boolean updated = accountDAO.updatePasswordFromResetEmail(passwordReset.accountId, PasswordUtil.encrypt(passwordResetAdmin.password), passwordReset.state);

        if(updated) {
            return Response.noContent().build();
        }

        return Response.serverError().build();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{email}/partner")
    public Account retrievePartnerAccount(@Auth final AccessToken accessToken,
                                          @PathParam("email") final String email){

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);

        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Account not found")).build());
        }

        final Optional<Long> partnerAccountIdOptional = deviceDAO.getPartnerAccountId(accountIdOptional.get());

        if (!partnerAccountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Partner account not found")).build());
        }

        return accountDAO.getById(partnerAccountIdOptional.get()).get();
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/count_by_created")
    public List<AccountCount> retrieveCountsByCreated(@Auth final AccessToken accessToken) {
        return accountAdminDAO.countByDate();

    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/timezone_history/{email}")
    public Map<DateTime, TimeZoneHistory> timeZoneHistoryList(@Auth final AccessToken accessToken,
                                                              @PathParam("email") final String email) {
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Account not found!")).build());
        }
        return timeZoneHistoryDAODynamoDB.getAllTimeZones(accountIdOptional.get());
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ring_history")
    public TimeHistory timeDiagnose(@Auth final AccessToken accessToken,
                                    @QueryParam("email") final String email,
                                    @QueryParam("start_time_millis") final long startMillis,
                                    @QueryParam("end_time_millis") final long endMillis){
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(this.accountDAO, email);

        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Account not found")).build());
        }

        final long accountId = accountIdOptional.get();
        final DateTime queryStart = new DateTime(startMillis, DateTimeZone.UTC);
        final DateTime queryEnd = new DateTime(endMillis, DateTimeZone.UTC);

        final List<DeviceAccountPair> deviceAccountPairs = this.deviceDAO.getSensesForAccountId(accountId);
        if(deviceAccountPairs.isEmpty()){
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "No Sense paired")).build());
        }

        final List<TimeZoneHistory> timeZoneHistories = this.timeZoneHistoryDAODynamoDB.getTimeZoneHistory(accountId, queryStart, queryEnd);
        final List<SmartAlarmHistory> smartAlarmHistories = this.smartAlarmLoggerDynamoDB.getSmartAlarmHistoryByScheduleTime(accountId, queryStart, queryEnd);
        final List<RingTime> ringTimeHistory = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceAccountPairs.get(deviceAccountPairs.size() - 1).externalDeviceId,
                accountId, queryStart, queryEnd);
        final TimeHistory history = new TimeHistory(timeZoneHistories, smartAlarmHistories, ringTimeHistory);
        return history;

    }

    @ScopesAllowed({OAuthScope.ZENDESK_EXTENSION})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/zendesk/{email}")
    public Long getAccountIdForZendesk(@Auth final AccessToken accessToken,
                                          @PathParam("email") final String email){

        LOGGER.debug("Looking account up by email {}", email);
        final String zendeskHeader = this.request.getHeader(HelloHttpHeader.ZENDESK);

        if (zendeskHeader == null || !zendeskHeader.equals("mixpanel")) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        final Optional<Account> accountByEmailOptional = accountDAO.getByEmail(email.toLowerCase());
        if (!accountByEmailOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Account not found").build());
        }
        return accountByEmailOptional.get().id.get();

    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recent_pairs")
    public ImmutableList<DeviceAccountPair> getMostRecentPairsQualifiedForDustCalibration(@Auth final AccessToken accessToken,
                                                                                          @QueryParam("limit") final Integer limitRaw,
                                                                                          @QueryParam("max_id") final Integer maxIdRaw,
                                                                                          @QueryParam("min_up_days") final Integer minUpDaysRaw){
        final Integer limit = limitRaw == null ? DeviceAdminDAOImpl.DEFAULT_ACCOUNT_DEVICE_MAP_LIMIT
                : Math.min(limitRaw, DeviceAdminDAOImpl.MAX_ACCOUNT_DEVICE_MAP_LIMIT);
        final Integer maxId = maxIdRaw == null ? Integer.MAX_VALUE : maxIdRaw;
        final Integer minUpDays = minUpDaysRaw == null ? DeviceAdminDAOImpl.DEFAULT_ACCOUNT_DEVICE_MAP_MIN_UP_DAYS : minUpDaysRaw;
        return deviceAdminDAO.getMostRecentPairsQualifiedForDustCalibration(limit, maxId, minUpDays);
    }

    private Account withPhotoMaybe(final Account account) {
        final Optional<ImmutableProfilePhoto> photo = profilePhotoStore.get(account.id.get());
        if(photo.isPresent()) {
            return Account.withProfilePhoto(account, photo.get().photo());
        }

        return account;
    }
}
