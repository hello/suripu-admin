package com.hello.suripu.admin.cli;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.db.FirmwareVersionMappingDAODynamoDB;
import com.hello.suripu.core.db.PillViewsDynamoDB;
import com.hello.suripu.core.db.SenseEventsDynamoDB;
import com.hello.suripu.core.db.TagStoreDAODynamoDB;
import com.hello.suripu.coredropwizard.configuration.NewDynamoDBConfiguration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class CreateDynamoDBTables extends ConfiguredCommand<SuripuAdminConfiguration> {

    public CreateDynamoDBTables() {
        super("create_dynamodb_tables", "Create dynamoDB tables");
    }

    @Override
    protected void run(Bootstrap<SuripuAdminConfiguration> bootstrap, Namespace namespace, SuripuAdminConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        createSenseEventsTable(configuration, awsCredentialsProvider);
        createFirmwareVersionsMappingTable(configuration, awsCredentialsProvider);
        createSenseLastSeen(configuration, awsCredentialsProvider);
        createPillLastSeen(configuration, awsCredentialsProvider);
        createFWUpgradePath(configuration, awsCredentialsProvider);
        createTags(configuration, awsCredentialsProvider);
    }

    private void createSenseEventsTable(final SuripuAdminConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.endpoints().get(DynamoDBTableName.SENSE_EVENTS));
        final String tableName = config.tables().get(DynamoDBTableName.SENSE_EVENTS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SenseEventsDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createFirmwareVersionsMappingTable(final SuripuAdminConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.endpoints().get(DynamoDBTableName.FIRMWARE_VERSIONS));
        final String tableName = config.tables().get(DynamoDBTableName.FIRMWARE_VERSIONS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = FirmwareVersionMappingDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createSenseLastSeen(final SuripuAdminConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.endpoints().get(DynamoDBTableName.SENSE_LAST_SEEN));
        final String tableName = config.tables().get(DynamoDBTableName.SENSE_LAST_SEEN);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = PillViewsDynamoDB.createLastSeenTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(tableName + " " + description.getTableStatus());
        }
    }


    private void createPillLastSeen(final SuripuAdminConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.endpoints().get(DynamoDBTableName.PILL_LAST_SEEN));
        final String tableName = config.tables().get(DynamoDBTableName.PILL_LAST_SEEN);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = PillViewsDynamoDB.createLastSeenTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(tableName + " " + description.getTableStatus());
        }
    }

    private void createFWUpgradePath(final SuripuAdminConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.endpoints().get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH));
        final String tableName = config.tables().get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {

        }
    }

    private void createTags(final SuripuAdminConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final NewDynamoDBConfiguration config = configuration.dynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.endpoints().get(DynamoDBTableName.TAGS));
        final String tableName = config.tables().get(DynamoDBTableName.TAGS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TagStoreDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(tableName + " " + description.getTableStatus());
        }
    }

}
