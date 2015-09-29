package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateCalibrationResponse {

    @JsonProperty("success")
    public Set<String> success = new HashSet<String>();

    @JsonProperty("failed_condition")
    public Set<String> failedCondition = new HashSet<String>();

    @JsonProperty("unprocessed")
    public Set<String> unprocessed = new HashSet<String>();

    private void addSuccessItem(final String successItem) {
        this.success.add(successItem);
    }

    private void addFailedConditionItem(final String failedConditionItem) {
        this.failedCondition.add(failedConditionItem);
    }

    private void addUnprocessedItem(final String unprocessedItem) {
        this.unprocessed.add(unprocessedItem);
    }


    public static UpdateCalibrationResponse createFromPutBatchForceResponse(final Map<String, Boolean> putBatchForceResponse) {

        final UpdateCalibrationResponse updateCalibrationResponse = new UpdateCalibrationResponse();
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

        final UpdateCalibrationResponse updateCalibrationResponse = new UpdateCalibrationResponse();
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
