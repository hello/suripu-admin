package com.hello.suripu.admin.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.NewDynamoDBConfiguration;
import com.hello.suripu.coredw8.configuration.GraphiteConfiguration;
import com.hello.suripu.coredw8.configuration.KinesisConfiguration;
import com.hello.suripu.coredw8.configuration.RedisConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import io.dropwizard.db.DatabaseConfiguration;
import jersey.repackaged.com.google.common.base.MoreObjects;

public class SuripuAdminConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("sensors_db")
    private DataSourceFactory sensorsDB = new DataSourceFactory();

    public DataSourceFactory getSensorsDB() {
        return sensorsDB;
    }

    @Valid
    @NotNull
    @JsonProperty("common_db")
    private DataSourceFactory commonDB = new DataSourceFactory();

    public DataSourceFactory getCommonDB() {
        return commonDB;
    }

    @Valid
    @NotNull
    @JsonProperty("insights_db")
    private DataSourceFactory insightsDB = new DataSourceFactory();
    public DataSourceFactory getInsightsDB() {
        return insightsDB;
    }

    @Valid
    @NotNull
    @JsonProperty("insights_dynamo_db")
    private NewDynamoDBConfiguration insightsDynamoDB;
    public NewDynamoDBConfiguration getInsightsDynamoDB() {
        return insightsDynamoDB;
    }

    @Valid
    @NotNull
    @JsonProperty("preferences_db")
    private NewDynamoDBConfiguration preferencesDynamoDB;

    public NewDynamoDBConfiguration getPreferencesDynamoDB() {
        return preferencesDynamoDB;
    }

    @Valid
    @JsonProperty("debug")
    private Boolean debug = Boolean.FALSE;

    public Boolean getDebug() {
        return debug;
    }

    @Valid
    @NotNull
    @JsonProperty("metrics_enabled")
    private Boolean metricsEnabled;

    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }


    @Valid
    @NotNull
    @JsonProperty("graphite")
    private GraphiteConfiguration graphite;

    public GraphiteConfiguration getGraphite() {
        return graphite;
    }


    @Valid
    @NotNull
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public KinesisConfiguration getKinesisConfiguration() {
        return kinesisConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("kinesis_logger")
    private KinesisLoggerConfiguration kinesisLoggerConfiguration;

    public KinesisLoggerConfiguration getKinesisLoggerConfiguration() {
        return kinesisLoggerConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("redis")
    private RedisConfiguration redisConfiguration;

    public RedisConfiguration getRedisConfiguration() {
        return redisConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("dynamodb")
    private NewDynamoDBConfiguration dynamoDBConfiguration;
    public NewDynamoDBConfiguration dynamoDBConfiguration(){
        return dynamoDBConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("sleep_score_db")
    private NewDynamoDBConfiguration sleepScoreDynamoDB;
    public NewDynamoDBConfiguration getSleepScoreDynamoDB() { return sleepScoreDynamoDB; }

    @Valid
    @NotNull
    @JsonProperty("sleep_score_version")
    private String sleepScoreVersion;
    public String getSleepScoreVersion() { return sleepScoreVersion; }
    
    @Valid
    @NotNull
    @JsonProperty("sleep_stats_db")
    private NewDynamoDBConfiguration sleepStatsDynamoDBConfiguration;
    public NewDynamoDBConfiguration getSleepStatsDynamoConfiguration(){
        return this.sleepStatsDynamoDBConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("sleep_stats_version")
    private String sleepStatsVersion;
    public String getSleepStatsVersion() {
        return this.sleepStatsVersion;
    }

    @Valid
    @NotNull
    @Max(600)
    @JsonProperty("token_expiration")
    private Long tokenExpiration;
    public Long getTokenExpiration() {return this.tokenExpiration;}

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("debug", debug)
                .add("include_metrics", metricsEnabled)
                .toString();
    }
}