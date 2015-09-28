package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateCalibrationResponse {

    @JsonProperty("success")
    private Set<String> success;

    @JsonProperty("failed_condition")
    private Set<String> failedCondition;

    @JsonProperty("unprocessed")
    private Set<String> unprocessed;

    public UpdateCalibrationResponse(final Set<String> success, final Set<String> failedCondition, final Set<String> unprocessed) {
        this.success = success;
        this.failedCondition = failedCondition;
        this.unprocessed = unprocessed;
    }

    private UpdateCalibrationResponse addSuccessItem(final String successItem) {
        this.success.add(successItem);
        return this;
    }

    private UpdateCalibrationResponse addFailedConditionItem(final String failedConditionItem) {
        this.failedCondition.add(failedConditionItem);
        return this;
    }

    private UpdateCalibrationResponse addUnprocessedItem(final String unprocessedItem) {
        this.unprocessed.add(unprocessedItem);
        return this;
    }

    private static UpdateCalibrationResponse createEmpty() {
        return new UpdateCalibrationResponse(new HashSet<String>(), new HashSet<String>(), new HashSet<String>());
    }
    public static UpdateCalibrationResponse createFromPutBatchForceResponse(final Map<String, Boolean> putBatchForceResponse) {
        final UpdateCalibrationResponse updateCalibrationResponse = UpdateCalibrationResponse.createEmpty();

        for (final Map.Entry<String, Boolean> item :  putBatchForceResponse.entrySet()) {
            if (item.getValue()) {
                updateCalibrationResponse.addSuccessItem(item.getKey());
            }
            else {
                updateCalibrationResponse.addUnprocessedItem(item.getKey());
            }
        }
        return updateCalibrationResponse;
    }

    public static UpdateCalibrationResponse createFromPutBatchResponse(final Map<String, Optional<Boolean>> putBatchResponse) {
        final UpdateCalibrationResponse updateCalibrationResponse = UpdateCalibrationResponse.createEmpty();

        for (final Map.Entry<String, Optional<Boolean>> item :  putBatchResponse.entrySet()) {
            if (!item.getValue().isPresent()) {
                updateCalibrationResponse.addFailedConditionItem(item.getKey());
            }
            else if (item.getValue().get()) {
                updateCalibrationResponse.addSuccessItem(item.getKey());
            }
            else {
                updateCalibrationResponse.addUnprocessedItem(item.getKey());
            }
        }
        return updateCalibrationResponse;
    }
}
