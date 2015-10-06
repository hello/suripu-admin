package com.hello.suripu.admin.resources.v1;

import com.hello.suripu.core.db.TimelineAnalyticsDAO;
import com.hello.suripu.core.models.GroupedTimelineLogSummary;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;


@Path("/v1/timelines")
public class TimelineResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResources.class);

    final TimelineAnalyticsDAO timelineAnalyticsDAO;

    public TimelineResources(final TimelineAnalyticsDAO timelineAnalyticsDAO) {
        this.timelineAnalyticsDAO = timelineAnalyticsDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Path("/summary/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupedTimelineLogSummary> getTimelineSummariesForNight(@Auth final AccessToken accessToken,
                                    @PathParam("date") final String dateOfNight) {
        List<GroupedTimelineLogSummary> summaries = timelineAnalyticsDAO.getGroupedSummary(dateOfNight);
        return summaries;
    }
}
