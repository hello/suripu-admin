package com.hello.suripu.admin.db;


import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.models.DeviceAccountPair;
import io.dropwizard.jdbi.ImmutableListContainerFactory;
import io.dropwizard.jdbi.OptionalContainerFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DeviceAdminDAOTest {

    private DBI dbi;
    private Handle handle;
    private DeviceAdminDAOImpl deviceAdminDAO;
    private DeviceDAO deviceDAO;

    @Before
    public void setUp() throws Exception {
        final String createTableAccountDeviceMapQuery = "CREATE TABLE account_device_map (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    account_id BIGINT,\n" +
                "    device_name VARCHAR(100),\n" +
                "    device_id VARCHAR(100),\n" +
                "    active BOOLEAN default true,\n" +
                "    last_updated TIMESTAMP default current_timestamp,\n" +
                "    created_at TIMESTAMP default current_timestamp\n" +
                ");";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new DeviceAccountPairMapper());
        dbi.registerContainerFactory(new ImmutableListContainerFactory());
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        dbi.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        handle = dbi.open();

        handle.execute(createTableAccountDeviceMapQuery);
        deviceAdminDAO = dbi.onDemand(DeviceAdminDAOImpl.class);
        deviceDAO = dbi.onDemand(DeviceDAO.class);
    }

    @After
    public void cleanUp() throws Exception {
        handle.execute("DROP TABLE account_device_map;");
        handle.close();
    }

    @Test
    public void testQuery() throws Exception {
        for (int i = 1; i <=10; i++) {
            deviceDAO.registerSense((long)i, String.format("sense%s", i));
        }
        final Integer limit = 2;
        final Integer max_id = 8;
        final List<DeviceAccountPair> senseAccountPairs = deviceAdminDAO.getMostRecentPairs(limit, max_id);
        assertThat(senseAccountPairs.size(), is(limit));
        for (int j = 0; j < limit; j++) {
            assertThat(senseAccountPairs.get(j).internalDeviceId, is((long) (max_id - j)));
            assertThat(senseAccountPairs.get(j).accountId, is((long) (max_id - j)));
            assertThat(senseAccountPairs.get(j).externalDeviceId, is(String.format("sense%s", (max_id - j))));
        }
    }
}
