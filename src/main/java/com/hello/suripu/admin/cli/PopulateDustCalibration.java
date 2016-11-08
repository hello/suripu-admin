package com.hello.suripu.admin.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.coredropwizard.clients.AmazonDynamoDBClientFactory;
import com.opencsv.CSVReader;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class PopulateDustCalibration extends ConfiguredCommand<SuripuAdminConfiguration> {


    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateDustCalibration.class);

    private static final Long CHINA_OFFSET_MILLIS = 8L * 3600L * 1000L; // GMT +8
    private static final int BATCH_SIZE = 50;

    public PopulateDustCalibration() {
        super("dusty", "dust calibration");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("--data")
                .nargs("?")
                .required(true)
                .help("data file with <sense-id>,<offset>,<created>");

    }

    @Override
    protected void run(Bootstrap<SuripuAdminConfiguration> bootstrap, Namespace namespace, SuripuAdminConfiguration configuration) throws Exception {

        final File dataFile = new File(namespace.getString("data"));
        final List<Calibration> calibrationList = getData(dataFile);
        LOGGER.debug("num_to_calibrate={}", calibrationList.size());

        final List<List<Calibration>> batches = Lists.partition(calibrationList, BATCH_SIZE);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());
        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();

        final AmazonDynamoDB calibrationDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.CALIBRATION, CalibrationDynamoDB.class);
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.create(calibrationDynamoDBClient, tableNames.get(DynamoDBTableName.CALIBRATION));

        int success = 0;
        int failures = 0;
        int failCondition = 0;
        int batchCount = 1;
        for (List<Calibration> batch : batches) {
            LOGGER.debug("processing_batch={} batch_size={}", batchCount, batch.size());
            final Map<String, Optional<Boolean>> responses = calibrationDAO.putBatch(batch);

            // process results
            for (final Map.Entry<String, Optional<Boolean>> item :  responses.entrySet()) {
                final String senseId = item.getKey();
                if (!item.getValue().isPresent()) {
                    LOGGER.error("error=unprocessed sense_id={}", senseId);
                    failures++;
                } else if (item.getValue().get()) {
                    LOGGER.debug("upload=success sense_id={}", senseId);
                    success++;
                } else {
                    LOGGER.debug("upload=fail-condition sense_id={}", senseId);
                    failCondition++;
                }
            }
            batchCount++;
        }

        LOGGER.info("success={}", success);
        LOGGER.info("failures={}", failures);
        LOGGER.info("fail_condition={}", failCondition);
    }


    /**
     * read calibration data from csv file
     * @param datafile file
     * @return list of Calibrations
     * @throws IOException yep
     */
    private List<Calibration> getData(final File datafile) throws IOException {
        final List<Calibration> calibrationList = Lists.newArrayList();
        try (final InputStream input = new FileInputStream(datafile);
             final CSVReader reader = new CSVReader(new InputStreamReader(input), ',')) {
            for (final String[] line: reader) {
                final String senseId = line[0];
                final Integer dustOffset = Integer.valueOf(line[1]);
                final Long testedAt = (Long.valueOf(line[2]) * 1000L) - CHINA_OFFSET_MILLIS; // convert to UTC
                final Calibration calibration = Calibration.create(senseId, dustOffset, testedAt);
                calibrationList.add(calibration);
            }
        }
        return calibrationList;
    }
}
