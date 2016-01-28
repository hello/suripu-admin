package com.hello.suripu.admin.store;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderMapper implements ResultSetMapper<Order> {

    @Override
    public Order map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return Order.create(r.getString("shipping_address_name"), r.getString("external_id"));
    }
}
