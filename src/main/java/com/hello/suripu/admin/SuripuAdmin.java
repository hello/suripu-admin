package com.hello.suripu.admin;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.admin.cli.CreateDynamoDBTables;
import com.hello.suripu.admin.cli.ManageKinesisStreams;
import com.hello.suripu.admin.cli.PopulateColors;
import com.hello.suripu.admin.cli.PopulateDustCalibration;
import com.hello.suripu.admin.cli.ScanFWVersion;
import com.hello.suripu.admin.cli.ScanSerialNumbers;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.admin.db.AccessTokenAdminDAO;
import com.hello.suripu.admin.db.AccountAdminDAO;
import com.hello.suripu.admin.db.DeviceAdminDAO;
import com.hello.suripu.admin.db.DeviceAdminDAOImpl;
import com.hello.suripu.admin.db.ExpansionsAdminDAO;
import com.hello.suripu.admin.db.RedshiftDAO;
import com.hello.suripu.admin.db.UptimeDAO;
import com.hello.suripu.admin.modules.AdminRolloutModule;
import com.hello.suripu.admin.processors.ActiveDevicesTracker;
import com.hello.suripu.admin.resources.v1.AccountResources;
import com.hello.suripu.admin.resources.v1.AggStatsResource;
import com.hello.suripu.admin.resources.v1.AlarmResources;
import com.hello.suripu.admin.resources.v1.ApplicationResources;
import com.hello.suripu.admin.resources.v1.CalibrationResources;
import com.hello.suripu.admin.resources.v1.DataResources;
import com.hello.suripu.admin.resources.v1.DeviceResources;
import com.hello.suripu.admin.resources.v1.DiagnosticResources;
import com.hello.suripu.admin.resources.v1.DownloadResource;
import com.hello.suripu.admin.resources.v1.EventsResources;
import com.hello.suripu.admin.resources.v1.ExpansionsResource;
import com.hello.suripu.admin.resources.v1.FeaturesResources;
import com.hello.suripu.admin.resources.v1.FeedbackResources;
import com.hello.suripu.admin.resources.v1.FileResources;
import com.hello.suripu.admin.resources.v1.FirmwareResource;
import com.hello.suripu.admin.resources.v1.InsightsResource;
import com.hello.suripu.admin.resources.v1.InspectionResources;
import com.hello.suripu.admin.resources.v1.KeyStoreResources;
import com.hello.suripu.admin.resources.v1.OnBoardingLogResource;
import com.hello.suripu.admin.resources.v1.PCHResources;
import com.hello.suripu.admin.resources.v1.PillResource;
import com.hello.suripu.admin.resources.v1.QuestionResources;
import com.hello.suripu.admin.resources.v1.TagsResources;
import com.hello.suripu.admin.resources.v1.TeamsResources;
import com.hello.suripu.admin.resources.v1.TimelineResources;
import com.hello.suripu.admin.resources.v1.TokenResources;
import com.hello.suripu.admin.resources.v1.TrackingResources;
import com.hello.suripu.admin.resources.v1.UptimeResources;
import com.hello.suripu.admin.resources.v1.VersionResources;
import com.hello.suripu.admin.resources.v1.WifiResources;
import com.hello.suripu.admin.store.StoreDAO;
import com.hello.suripu.admin.store.StoreResources;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AggStatsDAODynamoDB;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.FileManifestDynamoDB;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAODynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MarketingInsightsSeenDAODynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.PillViewsDynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.db.SenseEventsDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TagStoreDAODynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TimelineAnalyticsDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.WifiInfoDynamoDB;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDynamoDBDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.core.insights.InsightsLastSeenDAO;
import com.hello.suripu.core.insights.InsightsLastSeenDynamoDB;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.AggStatsProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.QuestionProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.core.profile.ProfilePhotoStore;
import com.hello.suripu.core.profile.ProfilePhotoStoreDynamoDB;
import com.hello.suripu.core.sense.metadata.MetadataDAODynamoDB;
import com.hello.suripu.core.sense.metadata.SenseMetadataDAO;
import com.hello.suripu.core.tracking.TrackingDAO;
import com.hello.suripu.coredropwizard.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.coredropwizard.db.AccessTokenDAO;
import com.hello.suripu.coredropwizard.db.AuthorizationCodeDAO;
import com.hello.suripu.coredropwizard.metrics.RegexMetricFilter;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import com.hello.suripu.coredropwizard.oauth.AuthDynamicFeature;
import com.hello.suripu.coredropwizard.oauth.AuthValueFactoryProvider;
import com.hello.suripu.coredropwizard.oauth.OAuthAuthenticator;
import com.hello.suripu.coredropwizard.oauth.OAuthAuthorizer;
import com.hello.suripu.coredropwizard.oauth.OAuthCredentialAuthFilter;
import com.hello.suripu.coredropwizard.oauth.ScopesAllowedDynamicFeature;
import com.hello.suripu.coredropwizard.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.coredropwizard.util.CustomJSONExceptionMapper;
import com.librato.rollout.RolloutClient;
import io.dropwizard.Application;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.net.InetSocketAddress;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class SuripuAdmin extends Application<SuripuAdminConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuAdmin.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
        new SuripuAdmin().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuAdminConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new CreateDynamoDBTables());
        bootstrap.addCommand(new ScanSerialNumbers());
        bootstrap.addCommand(new ScanFWVersion());
        bootstrap.addCommand(new PopulateColors());
        bootstrap.addCommand(new ManageKinesisStreams());
        bootstrap.addCommand(new PopulateDustCalibration());
    }

    @Override
    public void run(SuripuAdminConfiguration configuration, Environment environment) throws Exception {
        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql-common");
        final DBI redshiftDB = factory.build(environment, configuration.getRedshiftDB(), "postgresql-redshift");
        final DBI storeDB = factory.build(environment, configuration.getStoredDB(), "postgresql-store");
        environment.healthChecks().unregister("postgresql-store");

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        // not registering any additional container factory for redshift
        // not registering any additional container factory for store db

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.suripu-admin", apiKey, env);

            final ImmutableList<String> metrics = ImmutableList.copyOf(configuration.getGraphite().getIncludeMetrics());
            final RegexMetricFilter metricFilter = new RegexMetricFilter(metrics);

            final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHostName, 2003));

            final GraphiteReporter reporter = GraphiteReporter.forRegistry(environment.metrics())
                    .prefixedWith(prefix)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(metricFilter)
                    .build(graphite);
            reporter.start(interval, TimeUnit.SECONDS);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());

        final AmazonS3 s3Client = new AmazonS3Client(awsCredentialsProvider);

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider, clientConfiguration);

        // Common DB
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final AccountAdminDAO accountAdminDAO = commonDB.onDemand(AccountAdminDAO.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final DeviceReadDAO deviceReadDAO = commonDB.onDemand(DeviceReadDAO.class);
        final DeviceAdminDAO deviceAdminDAO = commonDB.onDemand(DeviceAdminDAOImpl.class);
        final FeedbackReadDAO feedbackReadDAO = commonDB.onDemand(FeedbackReadDAO.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);
        final OnBoardingLogDAO onBoardingLogDAO = commonDB.onDemand(OnBoardingLogDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);
        final TrackingDAO trackingDAO = commonDB.onDemand(TrackingDAO.class);
        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);
        final TimelineAnalyticsDAO timelineAnalyticsDAO = commonDB.onDemand(TimelineAnalyticsDAO.class);
        final ExpansionsAdminDAO expansionsAdminDAO = commonDB.onDemand(ExpansionsAdminDAO.class);


        // Redshift
        final UptimeDAO uptimeDAO = redshiftDB.onDemand(UptimeDAO.class);
        final RedshiftDAO redshiftDAO = redshiftDB.onDemand(RedshiftDAO.class);

        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();
        final AmazonDynamoDB mergedUserInfoDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.ALARM_INFO);
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(
                mergedUserInfoDynamoDBClient, configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.ALARM_INFO)
        );

        final AmazonDynamoDB senseKeyStoreDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SENSE_KEY_STORE);
        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                senseKeyStoreDynamoDBClient,
                tableNames.get(DynamoDBTableName.SENSE_KEY_STORE),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        final AmazonDynamoDB pillKeyStoreDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_KEY_STORE);
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(
                pillKeyStoreDynamoDBClient,
                tableNames.get(DynamoDBTableName.PILL_KEY_STORE),
                "9876543219876543".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );
        final AmazonDynamoDB passwordResetDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PASSWORD_RESET);
        final PasswordResetDB passwordResetDB = PasswordResetDB.create(
                passwordResetDynamoDBClient,
                tableNames.get(DynamoDBTableName.PASSWORD_RESET)
        );

        //Insights postgres
        final DBI insightsDBI = factory.build(environment, configuration.getInsightsDB(), "insights");

        final QuestionResponseDAO questionResponseDAO = insightsDBI.onDemand(QuestionResponseDAO.class);

        //Insights dynamodb

        final TrendsInsightsDAO trendsInsightsDAO = insightsDBI.onDemand(TrendsInsightsDAO.class);

        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_STATS);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                tableNames.get(DynamoDBTableName.SLEEP_STATS),
                configuration.getSleepStatsVersion());

        final AmazonDynamoDB dynamoDBScoreClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_SCORE);
        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBScoreClient,
                tableNames.get(DynamoDBTableName.SLEEP_SCORE),
                configuration.getSleepScoreVersion()
        );

        final AmazonDynamoDB insightsDynamoDB = dynamoDBClientFactory.getForTable(DynamoDBTableName.INSIGHTS);
        final InsightsDAODynamoDB insightsDAODynamoDB = new InsightsDAODynamoDB(insightsDynamoDB,
                tableNames.get(DynamoDBTableName.INSIGHTS));

        final AmazonDynamoDB insightsLastSeenDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.INSIGHTS_LAST_SEEN);
        final InsightsLastSeenDAO insightsLastSeenDAODynamoDB = InsightsLastSeenDynamoDB.create(insightsLastSeenDynamoDBClient,
                tableNames.get(DynamoDBTableName.INSIGHTS_LAST_SEEN));

        final AmazonDynamoDB accountPreferencesDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PREFERENCES);
        final AccountPreferencesDAO accountPreferencesDynamoDB = AccountPreferencesDynamoDB.create(accountPreferencesDynamoDBClient,
                tableNames.get(DynamoDBTableName.PREFERENCES));

        final SenseMetadataDAO senseMetadataDAO = MetadataDAODynamoDB.create(senseKeyStore);
        final SenseColorDAO senseColorDAO = new SenseColorDynamoDBDAO(senseMetadataDAO);

        //InsightsData
        final LightData lightData = new LightData(); // lights global distribution
        final WakeStdDevData wakeStdDevData = new WakeStdDevData();


        //Doing this programmatically instead of in config files
        AbstractServerFactory sf = (AbstractServerFactory) configuration.getServerFactory();
        // disable all default exception mappers
        sf.setRegisterDefaultExceptionMappers(false);

        environment.jersey().register(new CustomJSONExceptionMapper(configuration.getDebug()));

        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final AccessTokenAdminDAO accessTokenAdminDAO = commonDB.onDemand(AccessTokenAdminDAO.class);

        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final AuthorizationCodeDAO authCodeDAO = commonDB.onDemand(AuthorizationCodeDAO.class);

        final PersistentAccessTokenStore tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore, authCodeDAO);

        final PersistentAccessTokenStore implicitTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore, authCodeDAO, configuration.getTokenExpiration());

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());

        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, streams);
        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);

        environment.jersey().register(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<AccessToken>()
                .setAuthenticator(new OAuthAuthenticator(tokenStore))
                .setAuthorizer(new OAuthAuthorizer())
                .setRealm("SUPER SECRET STUFF")
                .setPrefix("Bearer")
                .setLogger(activityLogger)
                .buildAuthFilter()));
        environment.jersey().register(new ScopesAllowedDynamicFeature(applicationStore));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(AccessToken.class));

        final JedisPool jedisPool = new JedisPool(
                configuration.getRedisConfiguration().getHost(),
                configuration.getRedisConfiguration().getPort()
        );

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";

        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FEATURES, FeatureStore.class);
        final FeatureStore featureStore = new FeatureStore(
                featuresDynamoDBClient,
                tableNames.get(DynamoDBTableName.FEATURES),
                namespace
        );

        final AdminRolloutModule rolloutModule = new AdminRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(rolloutModule);

        environment.jersey().register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new RolloutClient(new DynamoDBAdapter(featureStore, 30))).to(RolloutClient.class);
            }
        });

        final AmazonDynamoDB teamStoreDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TEAMS, TeamStore.class);
        final TeamStore teamStore = new TeamStore(teamStoreDBClient, tableNames.get(DynamoDBTableName.TEAMS));

        final AmazonDynamoDB tagStoreDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TAGS, TagStoreDAODynamoDB.class);
        final TagStoreDAODynamoDB tagStore = new TagStoreDAODynamoDB(tagStoreDBClient, tableNames.get(DynamoDBTableName.TAGS));

        final AmazonDynamoDB senseEventsDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SENSE_EVENTS, SenseEventsDAO.class);
        final SenseEventsDAO senseEventsDAO = new SenseEventsDynamoDB(senseEventsDBClient, tableNames.get(DynamoDBTableName.SENSE_EVENTS));


        final AmazonDynamoDB fwVersionMapping = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FIRMWARE_VERSIONS, FirmwareVersionMappingDAO.class);
        final FirmwareVersionMappingDAO firmwareVersionMappingDAO = new FirmwareVersionMappingDAODynamoDB(fwVersionMapping, tableNames.get(DynamoDBTableName.FIRMWARE_VERSIONS));

        final AmazonDynamoDB otaHistoryClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.OTA_HISTORY, OTAHistoryDAODynamoDB.class);
        final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB = new OTAHistoryDAODynamoDB(otaHistoryClient, tableNames.get(DynamoDBTableName.OTA_HISTORY));

        final AmazonDynamoDB respCommandsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SYNC_RESPONSE_COMMANDS, ResponseCommandsDAODynamoDB.class);
        final ResponseCommandsDAODynamoDB respCommandsDAODynamoDB = new ResponseCommandsDAODynamoDB(respCommandsDynamoDBClient, tableNames.get(DynamoDBTableName.SYNC_RESPONSE_COMMANDS));

        final AmazonDynamoDB fwUpgradePathDynamoDB = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FIRMWARE_UPGRADE_PATH, FirmwareUpgradePathDAO.class);
        final FirmwareUpgradePathDAO firmwareUpgradePathDAO = new FirmwareUpgradePathDAO(fwUpgradePathDynamoDB, tableNames.get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH));

        final AmazonDynamoDBAsync sensorsViewsDynamoDBClient = new AmazonDynamoDBAsyncClient(AmazonDynamoDBClientFactory.getDefaultClientConfiguration());
        sensorsViewsDynamoDBClient.setEndpoint(configuration.dynamoDBConfiguration().defaultEndpoint());

        final SensorsViewsDynamoDB sensorsViewsDynamoDB = new SensorsViewsDynamoDB(
                sensorsViewsDynamoDBClient,
                tableNames.get(DynamoDBTableName.SENSE_PREFIX),
                tableNames.get(DynamoDBTableName.SENSE_LAST_SEEN)
        );


        final AmazonDynamoDB tzHistoryDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TIMEZONE_HISTORY, TimeZoneHistoryDAODynamoDB.class);
        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
                tzHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.TIMEZONE_HISTORY)
        );

        final AmazonDynamoDB smartAlarmHistoryDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SMART_ALARM_LOG, SmartAlarmLoggerDynamoDB.class);
        final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB = new SmartAlarmLoggerDynamoDB(
                smartAlarmHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.SMART_ALARM_LOG)
        );

        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.RING_TIME_HISTORY, RingTimeHistoryDAODynamoDB.class);
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(
                ringTimeHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.RING_TIME_HISTORY)
        );

        final AmazonDynamoDB pillViewsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.PILL_LAST_SEEN, PillViewsDynamoDB.class);
        final PillViewsDynamoDB pillViewsDynamoDB = new PillViewsDynamoDB(
                pillViewsDynamoDBClient,
                "", // TODO FIX THIS
                tableNames.get(DynamoDBTableName.PILL_LAST_SEEN)
        );

        final AmazonDynamoDB calibrationDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.CALIBRATION, CalibrationDynamoDB.class);
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.create(calibrationDynamoDBClient, tableNames.get(DynamoDBTableName.CALIBRATION));

        final AmazonDynamoDB wifiInfoDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.WIFI_INFO, WifiInfoDynamoDB.class);
        final WifiInfoDAO wifiInfoDAO = new WifiInfoDynamoDB(
                wifiInfoDynamoDBClient,
                tableNames.get(DynamoDBTableName.WIFI_INFO)
        );

        final AmazonDynamoDB pillHeartBeatDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.PILL_HEARTBEAT, PillHeartBeatDAODynamoDB.class);
        final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB = PillHeartBeatDAODynamoDB.create(
                pillHeartBeatDynamoDBClient,
                tableNames.get(DynamoDBTableName.PILL_HEARTBEAT)
        );

        final AmazonDynamoDB deviceDataDAODynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.DEVICE_DATA, DeviceDataDAODynamoDB.class);
        final DeviceDataDAODynamoDB deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(
                deviceDataDAODynamoDBClient,
                tableNames.get(DynamoDBTableName.DEVICE_DATA)
        );

        final AmazonDynamoDB marketingInsightsClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.MARKETING_INSIGHTS_SEEN, MarketingInsightsSeenDAODynamoDB.class);
        final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB = new MarketingInsightsSeenDAODynamoDB(
                marketingInsightsClient,
                tableNames.get(DynamoDBTableName.MARKETING_INSIGHTS_SEEN)
        );

        final AmazonDynamoDB fileManifestDAODynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FILE_MANIFEST, FileManifestDynamoDB.class);
        final FileManifestDynamoDB fileManifestDynamoDB = new FileManifestDynamoDB(
                fileManifestDAODynamoDBClient,
                tableNames.get(DynamoDBTableName.FILE_MANIFEST)
        ) ;

        final AmazonDynamoDB profilePhotoClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.PROFILE_PHOTO, ProfilePhotoStoreDynamoDB.class);
        final ProfilePhotoStore photoStore = ProfilePhotoStoreDynamoDB.create(profilePhotoClient, tableNames.get(DynamoDBTableName.PROFILE_PHOTO));

        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAODynamoDB, deviceReadDAO)
                .withSenseColorDAO(senseColorDAO)
                .withInsightsDAO(trendsInsightsDAO)
                .withDynamoDBDAOs(aggregateSleepScoreDAODynamoDB, insightsDAODynamoDB, insightsLastSeenDAODynamoDB, sleepStatsDAODynamoDB)
                .withPreferencesDAO(accountPreferencesDynamoDB)
                .withAccountReadDAO(accountDAO)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withLightData(lightData)
                .withWakeStdDevData(wakeStdDevData)
                .withCalibrationDAO(calibrationDAO)
                .withMarketingInsightsSeenDAO(marketingInsightsSeenDAODynamoDB);

        final InsightProcessor insightProcessor = insightBuilder.build();
        final ActiveDevicesTracker activeDevicesTracker = new ActiveDevicesTracker(jedisPool);


        environment.jersey().register(new InsightsResource(insightProcessor, deviceDAO, deviceDataDAODynamoDB));

        final AmazonDynamoDB pillDataDAODynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_DATA);
        final PillDataDAODynamoDB pillDataDAODynamoDB = new PillDataDAODynamoDB(
                pillDataDAODynamoDBClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.PILL_DATA)
        );


        //AggStats stuff
        final AmazonDynamoDB aggStatsDAODynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.AGG_STATS, AggStatsDAODynamoDB.class);
        final AggStatsDAODynamoDB aggStatsDAODynamoDB = new AggStatsDAODynamoDB(aggStatsDAODynamoDBClient,
                tableNames.get(DynamoDBTableName.AGG_STATS),
                configuration.getAggStatsVersion());

        final AggStatsProcessor.Builder aggStatsProcessorBuilder = new AggStatsProcessor.Builder()
                .withSleepStatsDAODynamoDB(sleepStatsDAODynamoDB)
                .withPillDataDAODynamoDB(pillDataDAODynamoDB)
                .withDeviceDataDAODynamoDB(deviceDataDAODynamoDB)
                .withSenseColorDAO(senseColorDAO)
                .withCalibrationDAO(calibrationDAO)
                .withAggStatsDAO(aggStatsDAODynamoDB);

        final AggStatsProcessor aggStatsProcessor = aggStatsProcessorBuilder.build();

        environment.jersey().register(new AggStatsResource(
                accountDAO,
                aggStatsProcessor,
                aggStatsDAODynamoDB,
                calibrationDAO,
                deviceReadDAO,
                redshiftDAO,
                senseColorDAO,
                sleepStatsDAODynamoDB));
        //End AggStats stuff


        environment.jersey().register(new AccountResources(accountDAO, passwordResetDB, deviceDAO, accountAdminDAO,
                timeZoneHistoryDAODynamoDB, smartAlarmLoggerDynamoDB, ringTimeHistoryDAODynamoDB, deviceAdminDAO, photoStore));

        environment.jersey().register(new AlarmResources(mergedUserInfoDynamoDB, deviceDAO, accountDAO));
        environment.jersey().register(new ApplicationResources(applicationStore));
        environment.jersey().register(new DataResources(deviceDataDAODynamoDB, deviceDAO, accountDAO, userLabelDAO, sensorsViewsDynamoDB, senseColorDAO, calibrationDAO, pillDataDAODynamoDB));
        final DeviceResources deviceResources = new DeviceResources(deviceDAO, deviceAdminDAO, pillDataDAODynamoDB, accountDAO,
                mergedUserInfoDynamoDB, senseKeyStore, pillKeyStore, jedisPool, pillHeartBeatDAO, senseColorDAO, respCommandsDAODynamoDB,pillViewsDynamoDB, sensorsViewsDynamoDB);

        environment.jersey().register(deviceResources);
        environment.jersey().register(new PillResource(accountDAO, pillHeartBeatDAODynamoDB, deviceDAO, deviceAdminDAO));
        environment.jersey().register(new DiagnosticResources(accountDAO, deviceDAO, trackingDAO, uptimeDAO));
        environment.jersey().register(new DownloadResource(s3Client, "hello-firmware"));
        environment.jersey().register(new EventsResources(senseEventsDAO));
        environment.jersey().register(new FeaturesResources(featureStore));
        environment.jersey().register(new FirmwareResource(
            jedisPool,
            firmwareVersionMappingDAO,
            otaHistoryDAODynamoDB,
            respCommandsDAODynamoDB,
            firmwareUpgradePathDAO,
            deviceDAO,
            teamStore,
            s3Client)
        );
        environment.jersey().register(new InspectionResources(deviceAdminDAO));
        environment.jersey().register(new OnBoardingLogResource(accountDAO, onBoardingLogDAO));
        environment.jersey().register(new TimelineResources(timelineAnalyticsDAO));
        environment.jersey().register(
                new PCHResources(
                        senseKeyStoreDynamoDBClient, // we use the same endpoint for Sense and Pill keystore
                        tableNames.get(DynamoDBTableName.SENSE_KEY_STORE),
                        tableNames.get(DynamoDBTableName.PILL_KEY_STORE),
                        senseColorDAO
                )
        );
        environment.jersey().register(new TeamsResources(teamStore));
        environment.jersey().register(new TagsResources(tagStore));
        environment.jersey().register(new TokenResources(implicitTokenStore, applicationStore, accessTokenDAO, accountDAO, accessTokenAdminDAO));
        environment.jersey().register(new CalibrationResources(calibrationDAO, deviceDAO, deviceDataDAODynamoDB));
        environment.jersey().register(new WifiResources(wifiInfoDAO));
        environment.jersey().register(new KeyStoreResources(senseKeyStore, pillKeyStore));
        environment.jersey().register(new FeedbackResources(feedbackReadDAO, feedbackDAO, accountDAO));
        environment.jersey().register(new TrackingResources(activeDevicesTracker));
        environment.jersey().register(new UptimeResources(teamStore, jedisPool));
        environment.jersey().register(new FileResources(fileManifestDynamoDB));
        environment.jersey().register(new VersionResources());
        environment.jersey().register(new ExpansionsResource(expansionsAdminDAO));
        // Store
        final StoreDAO storeDAO = storeDB.onDemand(StoreDAO.class);
        environment.jersey().register(new StoreResources(storeDAO));

        // questions
        final int numSkips = 5;
        final QuestionProcessor questionProcessor = new QuestionProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withCheckSkipsNum(numSkips)
                .withQuestions(questionResponseDAO)
                .build();
        environment.jersey().register(new QuestionResources(accountDAO, questionProcessor, timeZoneHistoryDAODynamoDB));

    }
}
