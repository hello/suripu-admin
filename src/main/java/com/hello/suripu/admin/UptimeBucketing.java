package com.hello.suripu.admin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.hello.suripu.core.diagnostic.Count;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.List;
import java.util.Map;

public class UptimeBucketing {

    private final static Ordering<Count> byMillisOrdering = new Ordering<Count>() {
        public int compare(Count left, Count right) {
            return Longs.compare(left.date.getMillis(), right.date.getMillis());
        }
    };


    /**
     * Creates a padded list starting from reference minus 10 days
     * and returns it sorted oldest to newest
     * @param reference
     * @param counts
     * @return
     */
    public static List<Count> padded(final DateTime reference, final ImmutableList<Count> counts) {
        final DateTime start = reference.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        final Hours hours = Hours.hoursBetween(start.minusDays(10), start);
        final Map<Long, Count> pad = Maps.newHashMap();
        for(int i =0; i < hours.getHours(); i++) {
            final DateTime dt = start.minusHours(i);
            pad.put(dt.getMillis(), new Count(dt, 0));
        }

        for(final Count c : counts) {
            pad.put(c.date.getMillis(), c);
        }

        return byMillisOrdering.sortedCopy(pad.values());
    }
}
