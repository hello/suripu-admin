package com.hello.suripu.admin.store;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentUpdate {

    @JsonProperty("order_id")
    public final String orderId;

    @JsonProperty("status")
    public final String status;

    @JsonProperty("error_message")
    public final String errorMessage;


    public PaymentUpdate(final String orderId, final String status, final String errorMessage) {
        this.orderId = orderId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static PaymentUpdate create(final String orderId, final String status, final String errorMessage) {
        return new PaymentUpdate(orderId, status, errorMessage);
    }
}
