package com.hello.suripu.admin;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.hello.suripu.core.diagnostic.Count;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Hours;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UptimeBucketingTest {

    @Test
    public void TestPadding() {


        final DateTime t1 = new DateTime(2016,1,1,0,0,0, DateTimeZone.UTC);
        final DateTime t2 = new DateTime(2016,1,1,1,0,0, DateTimeZone.UTC);
        final DateTime t3 = new DateTime(2016,1,2,1,0,0, DateTimeZone.UTC);

        final DateTime ref = new DateTime(2016,1,3,0,0,0, DateTimeZone.UTC);

        final List<Count> fromDB = Lists.newArrayList(
                new Count(t1, 10),
                new Count(t2, 1),
                new Count(t3, 100)
        );

        final List<Count> counts = UptimeBucketing.padded(ref, ImmutableList.copyOf(fromDB));
        assertThat(counts.size() > fromDB.size(), is(true));



        final Predicate<Count> notZero = new Predicate<Count>() {
            @Override public boolean apply(Count count) {
                return count.count != 0;
            }
        };

        final List<Count> nonZero = Lists.newArrayList(Iterables.filter(counts, notZero));
        assertThat(fromDB.size(), equalTo(nonZero.size()));


        final Hours hours = Hours.hoursBetween(ref, ref.minusDays(10));
        assertThat(counts.size(), equalTo(Math.abs(hours.getHours())));
    }




}
