package com.hello.suripu.admin.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.diagnostic.Count;
import com.hello.suripu.core.diagnostic.CountMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(CountMapper.class)
public interface UptimeDAO {

    @SqlQuery("SELECT date_trunc('hour', local_utc_ts) AS ts_hour, COUNT(*) AS cnt FROM (\n" +
            "SELECT distinct local_utc_ts FROM prod_sense_data WHERE \n" +
            "account_id = :account_id AND ts > current_date - interval '10 days') GROUP BY ts_hour ORDER BY ts_hour desc;")
    ImmutableList<Count> uptime(@Bind("account_id") Long accountId);
}
