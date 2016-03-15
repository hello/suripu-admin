package com.hello.suripu.admin.resources.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.Team;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/uptime")
public class UptimeResources {
    private static final String SENSE_UPTIME_HSET_KEY = "sense_uptimes";  // matches suripu-analytics

    public static class Uptime {
        @JsonProperty("device_id")
        public final String deviceId;

        @JsonProperty("uptime")
        public final Integer uptime;

        public Uptime(final String deviceId, final Integer uptime) {
            this.deviceId = deviceId;
            this.uptime = uptime;
        }
    }

    private final TeamStore teamStore;
    private final JedisPool jedisPool;

    public UptimeResources(final TeamStore teamStore, final JedisPool jedisPool) {
        this.teamStore = teamStore;
        this.jedisPool = jedisPool;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/{group}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Uptime> uptimeByFirmwareGroup(@Auth final AccessToken accessToken,
                                              @PathParam("group") final String groupName) {

        final Optional<Team> teamOptional = teamStore.getTeam(groupName, TeamStore.Type.DEVICES);
        if(!teamOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(),
                            String.format("Group: %s doesn't exist", groupName))).build());
        }

        // We need a list because Jedis returns a sorted list of values, not keys.
        final List<String> ids = Lists.newArrayList(teamOptional.get().ids);
        final List<String> uptimes = Lists.newArrayListWithCapacity(ids.size());
        final List<Uptime> merged = Lists.newArrayListWithCapacity(ids.size());

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            uptimes.addAll(jedis.hmget(SENSE_UPTIME_HSET_KEY, ids.toArray(new String[ids.size()])));

        } catch (JedisDataException e) {
            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            String.format("Failed to get data from redis - %s", e.getMessage()))).build());
        } catch (Exception e) {
            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            String.format("Failed to retrieve certified combined firmware versions because %s", e.getMessage()))).build());
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }


        for(int i = 0; i < ids.size(); i++) {
            final String deviceId = ids.get(i);
            final String uptime = uptimes.get(i);
            if (uptime != null) {
                merged.add(new Uptime(deviceId, Integer.valueOf(uptime)));
            }
        }

        return merged;
    }

}
