package com.hello.suripu.admin.db.mappers;

import com.hello.suripu.admin.models.AccessTokenAdmin;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;



public class AccessTokenAdminMapper implements ResultSetMapper<AccessTokenAdmin> {
    @Override
    public AccessTokenAdmin map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Array scopes = r.getArray("scopes");

        // TODO: Scopes is nullable, handle failure cases
        final Integer[] a = (Integer[]) scopes.getArray();
        final OAuthScope[] scopeArray = OAuthScope.fromIntegerArray(a);

        return new AccessTokenAdmin(
                r.getLong("id"),
                UUID.fromString(r.getString("access_token")),
                UUID.fromString(r.getString("refresh_token")),
                r.getLong("expires_in"),
                new DateTime(r.getTimestamp("created_at"), DateTimeZone.UTC),
                r.getLong("account_id"),
                r.getLong("app_id"),
                scopeArray

        );
    }
}
