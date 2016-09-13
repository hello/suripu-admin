package com.hello.suripu.admin.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.db.mappers.RedshiftAggStatDeviceDataMapper;
import com.hello.suripu.admin.db.mappers.RedshiftAggStatTrackerMotionMapper;
import com.hello.suripu.core.pill.data.AggStatTrackerMotion;
import com.hello.suripu.core.sense.data.AggStatDeviceData;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by jyfan on 8/26/16.
 */
public abstract class RedshiftDAO {

    @RegisterMapper(RedshiftAggStatTrackerMotionMapper.class)
    @SqlQuery("SELECT local_utc_ts FROM prod_pill_data WHERE account_id = :account_id AND local_utc_ts >= :start_local_time AND local_utc_ts <= :end_local_time ORDER BY local_utc_ts DESC;")
    public abstract ImmutableList<AggStatTrackerMotion> getPillDataBetweenLocalUTC(@Bind("account_id") final Long accountId,
                                                                                   @Bind("start_local_time") final DateTime startLocalTime,
                                                                                   @Bind("end_local_time") final DateTime endLocalTime);

    @RegisterMapper(RedshiftAggStatDeviceDataMapper.class)
    @SqlQuery("SELECT local_utc_ts, ambient_temp, ambient_humidity, ambient_air_quality, ambient_light FROM prod_sense_data WHERE account_id = :account_id AND local_utc_ts >= :start_local_time AND local_utc_ts <= :end_local_time ORDER BY local_utc_ts DESC;")
    public abstract ImmutableList<AggStatDeviceData> getSenseDataBetweenLocalUTC(@Bind("account_id") final Long accountId,
                                                                                 @Bind("start_local_time") final DateTime startLocalTime,
                                                                                 @Bind("end_local_time") final DateTime endLocalTime);


}
