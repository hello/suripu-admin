package com.hello.suripu.admin.db;

import com.hello.suripu.admin.db.mappers.AccessTokenAdminMapper;
import com.hello.suripu.admin.models.AccessTokenAdmin;
import com.hello.suripu.coredw8.db.AccessTokenDAO;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.List;


@RegisterMapper(AccessTokenAdminMapper.class)
public interface AccessTokenAdminDAO extends AccessTokenDAO {

    @SingleValueResult(AccessTokenAdmin.class)
    @SqlQuery("SELECT * FROM oauth_tokens WHERE expires_in > 0 ORDER BY id DESC LIMIT :limit;")
    List<AccessTokenAdmin> getMostRecentActiveTokens(@Bind("limit") final Integer limit);

    @SingleValueResult(AccessTokenAdmin.class)
    @SqlQuery("SELECT * FROM oauth_tokens WHERE expires_in > 0 AND id < :max_id  ORDER BY id DESC LIMIT :limit;")
    List<AccessTokenAdmin> getActiveTokensWithCursor(@Bind("max_id") final Long maxId, @Bind("limit") final Integer limit);

    @SqlUpdate("UPDATE oauth_tokens SET expires_in = :expires_in WHERE id = :id;")
    Integer updateExpiration(@Bind("id") final Long id, @Bind("expires_in") final Integer expiresIn);

    @SqlQuery("SELECT * FROM oauth_tokens WHERE account_id = :account_id AND expires_in > 0 ORDER BY id DESC LIMIT :limit;")
    List<AccessTokenAdmin> getActiveTokensByAccountId(@Bind("account_id") final Long accountId, @Bind("limit") final Integer limit);
}
