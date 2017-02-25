package com.hello.suripu.admin.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.mappers.AccountCountMapper;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountCount;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.List;
import java.util.UUID;

@RegisterMapper(AccountMapper.class)
public interface AccountAdminDAO {

    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM accounts WHERE external_id=:external_id")
    Optional<Account> getByExternalId(@Bind("external_id") UUID uuid);


    @RegisterMapper(AccountCountMapper.class)
    @SqlQuery("SELECT date_trunc('day', created) AS created_date, COUNT(*) FROM accounts GROUP BY created_date ORDER BY created_date DESC;")
    List<AccountCount> countByDate();

    @SqlQuery("SELECT * FROM accounts WHERE id < :max_id ORDER BY id DESC LIMIT :limit;")
    List<Account> getRecentBeforeId(@Bind("limit") final Integer limit, @Bind("max_id") final Integer maxId);
}
