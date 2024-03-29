package com.hello.suripu.admin.store;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("email")
    public final String email;

    @JsonProperty("order_id")
    public final String orderId;

    public Order(final String name, final String email, final String orderId) {
        this.name = name;
        this.email = email;
        this.orderId = orderId;
    }

    public static Order create(final String name, final String email, final String orderId) {
        return new Order(name, email, orderId);
    }
}
