package com.hello.suripu.admin.modules;

import com.hello.suripu.admin.resources.v1.InsightsResource;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.core.processors.QuestionProcessor;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by kingshy on 2/8/16.
 */

@Module(injects = {
    QuestionProcessor.class,
    InsightsResource.class
})

public class AdminRolloutModule {
    private final FeatureStore featureStore;
    private final Integer pollingIntervalInSeconds;

    public AdminRolloutModule(final FeatureStore featureStore, final Integer pollingIntervalInSeconds) {
        this.featureStore = featureStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
    }

    @Provides @Singleton
    RolloutAdapter providesRolloutAdapter() {
        return new DynamoDBAdapter(featureStore, pollingIntervalInSeconds);
    }

    @Provides
    @Singleton
    RolloutClient providesRolloutClient(RolloutAdapter adapter) {
        return new RolloutClient(adapter);
    }

}
