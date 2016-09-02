package com.hello.suripu.admin.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.db.mappers.RedshiftDeviceDataMapper;
import com.hello.suripu.admin.db.mappers.RedshiftTrackerMotionMapper;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by jyfan on 8/26/16.
 */
public abstract class RedshiftDAO {

    @RegisterMapper(RedshiftTrackerMotionMapper.class)
    @SqlQuery("SELECT * FROM prod_pill_data WHERE account_id = :account_id AND local_utc_ts >= :start_local_time AND local_utc_ts <= :end_local_time ORDER BY ts DESC;")
    public abstract ImmutableList<TrackerMotion> getPillDataBetweenLocalUTC(@Bind("account_id") final Long accountId,
                                                                            @Bind("start_local_time") final DateTime startLocalTime,
                                                                            @Bind("end_local_time") final DateTime endLocalTime);

    @RegisterMapper(RedshiftDeviceDataMapper.class)
    @SqlQuery("SELECT * FROM prod_sense_data WHERE account_id = :account_id AND local_utc_ts >= :start_local_time AND local_utc_ts <= :end_local_time ORDER BY ts DESC;")
    public abstract ImmutableList<DeviceData> getSenseDataBetweenLocalUTC(@Bind("account_id") final Long accountId,
                                                                          @Bind("start_local_time") final DateTime startLocalTime,
                                                                          @Bind("end_local_time") final DateTime endLocalTime);


}
