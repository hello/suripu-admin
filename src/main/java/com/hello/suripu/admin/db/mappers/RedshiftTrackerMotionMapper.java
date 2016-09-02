package com.hello.suripu.admin.db.mappers;

import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jyfan on 8/29/16.
 */
public class RedshiftTrackerMotionMapper implements ResultSetMapper<TrackerMotion> {

    private static Long FAKE_LONG = 99L;

    @Override
    public TrackerMotion map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {

        final TrackerMotion.Builder builder = new TrackerMotion.Builder();
        builder.withId(FAKE_LONG);
        builder.withAccountId(resultSet.getLong("account_id"));
        builder.withTrackerId(FAKE_LONG);
        builder.withTimestampMillis(new DateTime(resultSet.getTimestamp("ts"), DateTimeZone.UTC).withSecondOfMinute(0).getMillis());
        builder.withOffsetMillis(resultSet.getInt("offset_millis"));
        builder.withValue(resultSet.getInt("svm_no_gravity"));
        builder.withMotionRange(resultSet.getLong("motion_range"));
        builder.withKickOffCounts(resultSet.getLong("kickoff_counts"));
        builder.withOnDurationInSeconds(resultSet.getLong("on_duration_seconds"));

        return builder.build();
    }
}

