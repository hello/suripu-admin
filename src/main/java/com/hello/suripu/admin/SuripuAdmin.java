package com.hello.suripu.admin;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3Client;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.suripu.admin.cli.CreateDynamoDBTables;
import com.hello.suripu.admin.cli.ManageKinesisStreams;
import com.hello.suripu.admin.cli.PopulateColors;
import com.hello.suripu.admin.cli.ScanFWVersion;
import com.hello.suripu.admin.cli.ScanSerialNumbers;
import com.hello.suripu.admin.resources.v1.InsightsResource;
import com.hello.suripu.admin.resources.v1.WifiResources;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.coredw8.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.coredw8.db.AccessTokenDAO;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.AuthDynamicFeature;
import com.hello.suripu.coredw8.oauth.AuthValueFactoryProvider;
import com.hello.suripu.coredw8.oauth.OAuthAuthenticator;
import com.hello.suripu.coredw8.oauth.OAuthAuthorizer;
import com.hello.suripu.coredw8.oauth.OAuthCredentialAuthFilter;
import com.hello.suripu.coredw8.oauth.ScopesAllowedDynamicFeature;
import com.hello.suripu.coredw8.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.admin.resources.v1.AccountResources;
import com.hello.suripu.admin.resources.v1.AlarmResources;
import com.hello.suripu.admin.resources.v1.ApplicationResources;
import com.hello.suripu.admin.resources.v1.CalibrationResources;
import com.hello.suripu.admin.resources.v1.DataResources;
import com.hello.suripu.admin.resources.v1.DeviceResources;
import com.hello.suripu.admin.resources.v1.DiagnosticResources;
import com.hello.suripu.admin.resources.v1.DownloadResource;
import com.hello.suripu.admin.resources.v1.EventsResources;
import com.hello.suripu.admin.resources.v1.FeaturesResources;
import com.hello.suripu.admin.resources.v1.FirmwareResource;
import com.hello.suripu.admin.resources.v1.InspectionResources;
import com.hello.suripu.admin.resources.v1.OnBoardingLogResource;
import com.hello.suripu.admin.resources.v1.PCHResources;
import com.hello.suripu.admin.resources.v1.TeamsResources;
import com.hello.suripu.admin.resources.v1.TokenResources;
import com.hello.suripu.coredw8.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOAdmin;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDAOAdmin;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.PillViewsDynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.diagnostic.DiagnosticDAO;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.tracking.TrackingDAO;
import io.dropwizard.Application;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.ImmutableListContainerFactory;
import io.dropwizard.jdbi.ImmutableSetContainerFactory;
import io.dropwizard.jdbi.OptionalContainerFactory;
import io.dropwizard.jdbi.args.OptionalArgumentFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
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
    }

    @Override
    public void run(SuripuAdminConfiguration configuration, Environment environment) throws Exception {
        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql-common");
        final DBI sensorsDB = factory.build(environment, configuration.getSensorsDB(), "postgresql-sensors");

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.suripu-admin", apiKey, env);

            final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHostName, 2003));

            final GraphiteReporter reporter = GraphiteReporter.forRegistry(environment.metrics())
                    .prefixedWith(prefix)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(graphite);
            reporter.start(interval, TimeUnit.SECONDS);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());

        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentialsProvider);


        // Common DB
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final AccountDAOAdmin accountDAOAdmin = commonDB.onDemand(AccountDAOAdmin.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final DeviceDAOAdmin deviceDAOAdmin = commonDB.onDemand(DeviceDAOAdmin.class);
        final OnBoardingLogDAO onBoardingLogDAO = commonDB.onDemand(OnBoardingLogDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);
        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        final TrackingDAO trackingDAO = commonDB.onDemand(TrackingDAO.class);
        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);

        // Sensor DB
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final DiagnosticDAO diagnosticDAO = sensorsDB.onDemand(DiagnosticDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);

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

        final AmazonDynamoDB accountPreferencesDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PREFERENCES);
        final AccountPreferencesDAO accountPreferencesDynamoDB = AccountPreferencesDynamoDB.create(accountPreferencesDynamoDBClient,
                tableNames.get(DynamoDBTableName.PREFERENCES));

        //InsightsData
        final LightData lightData = new LightData(); // lights global distribution
        final WakeStdDevData wakeStdDevData = new WakeStdDevData();


        //Doing this programmatically instead of in config files
        AbstractServerFactory sf = (AbstractServerFactory) configuration.getServerFactory();
        // disable all default exception mappers
        sf.setRegisterDefaultExceptionMappers(false);

        environment.jersey().register(new CustomJSONExceptionMapper(configuration.getDebug()));

        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);

        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final PersistentAccessTokenStore tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);

        environment.jersey().register(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<AccessToken>()
                .setAuthenticator(new OAuthAuthenticator(tokenStore))
                .setAuthorizer(new OAuthAuthorizer())
                .setRealm("SUPER SECRET STUFF")
                .setPrefix("Bearer")
                .buildAuthFilter()));
        environment.jersey().register(ScopesAllowedDynamicFeature.class);
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

        final AmazonDynamoDB teamStoreDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TEAMS, TeamStore.class);
        final TeamStore teamStore = new TeamStore(teamStoreDBClient, tableNames.get(DynamoDBTableName.TEAMS));

        final AmazonDynamoDB senseEventsDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SENSE_EVENTS, SenseEventsDAO.class);
        final SenseEventsDAO senseEventsDAO = new SenseEventsDAO(senseEventsDBClient, tableNames.get(DynamoDBTableName.SENSE_EVENTS));


        final AmazonDynamoDB fwVersionMapping = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FIRMWARE_VERSIONS, FirmwareVersionMappingDAO.class);
        final FirmwareVersionMappingDAO firmwareVersionMappingDAO = new FirmwareVersionMappingDAO(fwVersionMapping, tableNames.get(DynamoDBTableName.FIRMWARE_VERSIONS));

        final AmazonDynamoDB otaHistoryClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.OTA_HISTORY, OTAHistoryDAODynamoDB.class);
        final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB = new OTAHistoryDAODynamoDB(otaHistoryClient, tableNames.get(DynamoDBTableName.OTA_HISTORY));

        final AmazonDynamoDB respCommandsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SYNC_RESPONSE_COMMANDS, ResponseCommandsDAODynamoDB.class);
        final ResponseCommandsDAODynamoDB respCommandsDAODynamoDB = new ResponseCommandsDAODynamoDB(respCommandsDynamoDBClient, tableNames.get(DynamoDBTableName.SYNC_RESPONSE_COMMANDS));

        final AmazonDynamoDB fwUpgradePathDynamoDB = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FIRMWARE_UPGRADE_PATH, FirmwareUpgradePathDAO.class);
        final FirmwareUpgradePathDAO firmwareUpgradePathDAO = new FirmwareUpgradePathDAO(fwUpgradePathDynamoDB, tableNames.get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH));

        final AmazonDynamoDB sensorsViewsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SENSE_LAST_SEEN, SensorsViewsDynamoDB.class);
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
        final CalibrationDAO calibrationDAO = new CalibrationDynamoDB(
                calibrationDynamoDBClient,
                tableNames.get(DynamoDBTableName.CALIBRATION)
        );

        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDAO)
                .withTrackerMotionDAO(trackerMotionDAO)
                .withInsightsDAO(trendsInsightsDAO)
                .withDynamoDBDAOs(aggregateSleepScoreDAODynamoDB, insightsDAODynamoDB, sleepStatsDAODynamoDB)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withLightData(lightData)
                .withWakeStdDevData(wakeStdDevData)
                .withPreferencesDAO(accountPreferencesDynamoDB);

        final InsightProcessor insightProcessor = insightBuilder.build();

        environment.jersey().register(new InsightsResource(insightProcessor, deviceDAO));


        environment.jersey().register(PingResource.class);
        environment.jersey().register(new AccountResources(accountDAO, passwordResetDB, deviceDAO, accountDAOAdmin,
                timeZoneHistoryDAODynamoDB, smartAlarmLoggerDynamoDB, ringTimeHistoryDAODynamoDB));
        environment.jersey().register(new AlarmResources(mergedUserInfoDynamoDB, deviceDAO, accountDAO));
        environment.jersey().register(new ApplicationResources(applicationStore));
        environment.jersey().register(new DataResources(deviceDataDAO, deviceDAO, accountDAO, userLabelDAO, trackerMotionDAO, sensorsViewsDynamoDB, senseColorDAO, calibrationDAO));
        final DeviceResources deviceResources = new DeviceResources(deviceDAO, deviceDAOAdmin, deviceDataDAO, trackerMotionDAO, accountDAO,
                mergedUserInfoDynamoDB, senseKeyStore, pillKeyStore, jedisPool, pillHeartBeatDAO, senseColorDAO, respCommandsDAODynamoDB,pillViewsDynamoDB, sensorsViewsDynamoDB);

        environment.jersey().register(deviceResources);
        environment.jersey().register(new DiagnosticResources(diagnosticDAO, accountDAO, deviceDAO, trackingDAO));
        environment.jersey().register(new DownloadResource(s3Client, "hello-firmware"));
        environment.jersey().register(new EventsResources(senseEventsDAO));
        environment.jersey().register(new FeaturesResources(featureStore));
        environment.jersey().register(new FirmwareResource(jedisPool, firmwareVersionMappingDAO, otaHistoryDAODynamoDB, respCommandsDAODynamoDB, firmwareUpgradePathDAO, deviceDAO, sensorsViewsDynamoDB, teamStore));
        environment.jersey().register(new InspectionResources(deviceDAOAdmin));
        environment.jersey().register(new OnBoardingLogResource(accountDAO, onBoardingLogDAO));
        environment.jersey().register(
                new PCHResources(
                        senseKeyStoreDynamoDBClient, // we use the same endpoint for Sense and Pill keystore
                        tableNames.get(DynamoDBTableName.SENSE_KEY_STORE),
                        tableNames.get(DynamoDBTableName.PILL_KEY_STORE),
                        senseColorDAO
                )
        );
        environment.jersey().register(new TeamsResources(teamStore));
        environment.jersey().register(new TokenResources(tokenStore, applicationStore, accessTokenDAO, accountDAO));
        environment.jersey().register(new CalibrationResources(calibrationDAO));
        environment.jersey().register(new WifiResources(jedisPool));
    }
}
