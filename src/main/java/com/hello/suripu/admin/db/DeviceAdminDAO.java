package com.hello.suripu.admin.db;


import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;

public interface DeviceAdminDAO {
    ImmutableList<DeviceStatus> pillStatusBeforeTs(final Long pillId, final DateTime endTs, final Integer limit);
    ImmutableList<Account> getAccountsBySenseId(final String deviceId, final Long maxDevices);
    ImmutableList<Account> getAccountsByPillId(final String deviceId, final Long maxDevices);
    ImmutableList<DeviceAccountPair> getPillsByPillIdHint(final String pillId);
    ImmutableList<Account> getAccountsWithSenseWithoutPill(final Integer limit);
    ImmutableList<DeviceAccountPair> getMostRecentPairs(Integer limit, Integer maxId);
    ImmutableList<Account> getAccountsWithLowPillBattery(final Integer criticalBatteryLevel, final Integer limit);
    ImmutableList<DeviceAccountPair> getMostRecentPairsQualifiedForDustCalibration(final Integer limit, final Integer maxId, final Integer minUpDays);
    ImmutableList<DeviceAccountPair> getLatestUniqueActiveSensePairs(final Integer maxId, final Integer limit);
    ImmutableList<DeviceAccountPair> getLatestUniqueActivePillPairs(final Integer maxId, final Integer limit);
    Long getAllSensesCount();
    Long getAllPillsCount();

}
