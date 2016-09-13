package com.hello.suripu.admin.db.mappers;

import com.hello.suripu.core.sense.data.AggStatDeviceData;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jyfan on 8/29/16.
 */
public class RedshiftAggStatDeviceDataMapper implements ResultSetMapper<AggStatDeviceData> {

    @Override
    public AggStatDeviceData map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        //Get localTime
        final DateTime localTime = new DateTime(r.getTimestamp("local_utc_ts"));

        //Get float lux
        final int lux = r.getInt("ambient_light");
        float fLux;
        if (localTime.getYear() <= 2014 ) {
            fLux = (float) lux;
        } else {
            fLux = DataUtils.convertLightCountsToLux(lux);
        }

        final AggStatDeviceData aggStatDeviceData = new AggStatDeviceData(
                r.getInt("ambient_temp"),
                r.getInt("ambient_humidity"),
                r.getInt("ambient_air_quality"),
                fLux,
                localTime);

        return aggStatDeviceData;
    }

}