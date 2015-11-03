package com.hello.suripu.admin.db;

import com.google.common.collect.ImmutableList;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

public abstract class TableDAOPostgres implements TableDAO {

    @SqlQuery("SELECT table_name FROM information_schema.tables where table_schema = 'public' ORDER BY table_name;")
    public abstract ImmutableList<String> tables();
}
