package com.hello.suripu.admin.store;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;


public interface StoreDAO {

    @RegisterMapper(OrderMapper.class)
    @SqlQuery("SELECT * FROM preorders WHERE external_id ILIKE '%'||:query||'%' OR shipping_address_name ILIKE '%'||:query||'%' OR email ILIKE '%'||:query||'%';")
    List<Order> searchOrder(final @Bind("query") String query);

    @RegisterMapper(PaymentUpdateMapper.class)
    @SqlQuery("SELECT * FROM payment_updates WHERE external_id = :order_id order by id asc")
    List<PaymentUpdate> paymentUpdates(final @Bind("order_id") String orderId);
}
