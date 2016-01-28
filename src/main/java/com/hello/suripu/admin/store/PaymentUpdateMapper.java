package com.hello.suripu.admin.store;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PaymentUpdateMapper implements ResultSetMapper<PaymentUpdate>{
    @Override
    public PaymentUpdate map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return PaymentUpdate.create(
                r.getString("external_id"),
                r.getString("status"),
                r.getString("error_message")
        );
    }
}
