package com.hello.suripu.admin.db.mappers;

import com.hello.suripu.core.pill.data.AggStatTrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jyfan on 8/29/16.
 */
public class RedshiftAggStatTrackerMotionMapper implements ResultSetMapper<AggStatTrackerMotion> {

    @Override
    public AggStatTrackerMotion map(int i, ResultSet r, StatementContext statementContext) throws SQLException {

        //Get localTime
        final DateTime localTime = new DateTime(r.getTimestamp("local_utc_ts"), DateTimeZone.UTC);

        final AggStatTrackerMotion aggStatTrackerMotion = new AggStatTrackerMotion(localTime);
        return aggStatTrackerMotion;
    }

}

