package com.hello.suripu.admin.resources.v1;


import com.codahale.metrics.annotation.Timed;
import com.hello.suripu.admin.models.WifiInfo;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Path("/v1/wifi")
public class WifiResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(WifiResources.class);
    public static final String WIFI_INFO_HASH_KEY = "wifi_info";

    private final JedisPool jedisPool;

    public WifiResources(final JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{sense_id}")
    public WifiInfo getAlarms(@Auth final AccessToken token,
                              @PathParam("sense_id") final String senseId ){


        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return WifiInfo.createWithRedisResult(jedis.hget(WIFI_INFO_HASH_KEY, senseId));
        }
        catch (JedisDataException e) {
            LOGGER.error("Redis data exception {}", e.getMessage());
            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
        }
        catch (Exception e) {
            LOGGER.error("Redis unknown exception", e.getMessage());
            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
        }
        finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        String.format("Failed to retrieve wifi info for %s", senseId))).build());
    }
}
