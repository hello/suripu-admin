package com.hello.suripu.admin.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

import java.util.List;

public interface ExpansionsAdminDAO {

    @SqlQuery("SELECT e.service_name  FROM expansion_data d LEFT JOIN expansions e ON e.id = d.app_id WHERE d.device_id= :sense_id AND d.enabled = true order by e.id;")
    List<String> getActiveExpansions(@Bind("sense_id") String senseId);
}
