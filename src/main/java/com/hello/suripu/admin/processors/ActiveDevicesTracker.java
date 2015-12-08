package com.hello.suripu.admin.processors;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Set;


public class ActiveDevicesTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveDevicesTracker.class);
    private final JedisPool jedisPool;

    public ActiveDevicesTracker(final JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Optional<Set<String>> getDiff(final String beforeSetKey, final String afterSetKey) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return Optional.of(jedis.sdiff(beforeSetKey, afterSetKey));
        }catch (final JedisDataException jde) {
            LOGGER.error("Failed getting data out of redis: {}", jde.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch(final Exception e) {
            LOGGER.error("Unknown error connection to redis: {}", e.getMessage());
            jedisPool.returnBrokenResource(jedis);
        }
        finally {
            try{
                jedisPool.returnResource(jedis);
            }catch (final JedisConnectionException jce) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }
        return Optional.absent();
    }
}
