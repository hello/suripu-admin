package com.hello.suripu.admin.resources.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.codahale.metrics.annotation.Timed;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB.ResponseCommand;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.FirmwareCountInfo;
import com.hello.suripu.core.models.FirmwareInfo;
import com.hello.suripu.core.models.OTAHistory;
import com.hello.suripu.core.models.Team;
import com.hello.suripu.core.models.UpgradeNodeRequest;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Path("/v1/firmware")
public class FirmwareResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareResource.class);

    private final FirmwareVersionMappingDAO firmwareVersionMappingDAO;
    final FirmwareUpgradePathDAO firmwareUpgradePathDAO;
    private final OTAHistoryDAODynamoDB otaHistoryDAO;
    private final ResponseCommandsDAODynamoDB responseCommandsDAODynamoDB;
    private final TeamStore teamStore;
    private final DeviceDAO deviceDAO;
    private final JedisPool jedisPool;
    private final AmazonS3 s3Client;

    private static final String FIRMWARES_SEEN_SET_KEY = "firmwares_seen";
    private static final String CERTIFIED_FIRMWARE_SET_KEY = "certified_firmware";
    private static final String DEVICE_KEY_BASE = "device_id:";

    public FirmwareResource(final JedisPool jedisPool,
                            final FirmwareVersionMappingDAO firmwareVersionMappingDAO,
                            final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB,
                            final ResponseCommandsDAODynamoDB responseCommandsDAODynamoDB,
                            final FirmwareUpgradePathDAO firmwareUpgradePathDAO,
                            final DeviceDAO deviceDAO,
                            final TeamStore teamStore,
                            final AmazonS3 s3Client) {
        this.jedisPool = jedisPool;
        this.firmwareVersionMappingDAO = firmwareVersionMappingDAO;
        this.otaHistoryDAO = otaHistoryDAODynamoDB;
        this.responseCommandsDAODynamoDB = responseCommandsDAODynamoDB;
        this.firmwareUpgradePathDAO = firmwareUpgradePathDAO;
        this.deviceDAO = deviceDAO;
        this.teamStore = teamStore;
        this.s3Client = s3Client;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareInfo> getFirmwareDeviceList(@Auth final AccessToken accessToken,
                                              @QueryParam("firmware_version") final Long firmwareVersion,
                                              @QueryParam("range_start") final Long rangeStart,
                                              @QueryParam("range_end") final Long rangeEnd) {
        if(firmwareVersion == null) {
            LOGGER.error("Missing firmwareVersion parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(rangeStart == null) {
            LOGGER.error("Missing range_start parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if(rangeEnd == null) {
            LOGGER.error("Missing range_end parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final String middleFWVersion = firmwareVersion.toString();
        final List<FirmwareInfo> deviceInfo = Lists.newArrayList();
        try {
            //Get all elements in the index range provided
            final Set<Tuple> allFWDevices = jedis.zrevrangeWithScores(middleFWVersion, rangeStart, rangeEnd);
            final Pipeline pipe = jedis.pipelined();
            final Map<String, redis.clients.jedis.Response<String>> responseMap = Maps.newHashMap();
            for(final Tuple device: allFWDevices){
                final String deviceId = device.getElement();
                responseMap.put(device.getElement(), pipe.hget(DEVICE_KEY_BASE.concat(deviceId), "top_version"));
            }
            pipe.sync();
            for (final Tuple device:allFWDevices) {
                final String deviceId = device.getElement();
                final String topFWVersion = (responseMap.get(deviceId) != null) ? responseMap.get(deviceId).get() : "0";
                final long lastSeen = (long) device.getScore();
                deviceInfo.add(new FirmwareInfo(middleFWVersion, topFWVersion, deviceId, lastSeen));
            }

        } catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch (Exception exception) {
            LOGGER.error("Failed retrieving FW list: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } finally {
            try {
                jedisPool.returnResource(jedis);
            } catch (JedisConnectionException e) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }

        return deviceInfo;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Long getFirmwareDeviceCount(@Auth final AccessToken accessToken,
                                       @QueryParam("firmware_version") final Long firmwareVersion) {
        if(firmwareVersion == null) {
            LOGGER.error("Missing firmwareVersion parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final String fwVersion = firmwareVersion.toString();
        Long devicesOnFirmware = 0L;

        try {
            devicesOnFirmware = jedis.zcard(fwVersion);

        } catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch (Exception exception) {
            LOGGER.error("Failed retrieving FW device count: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } finally {
            try {
                jedisPool.returnResource(jedis);
            } catch (JedisConnectionException e) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }

        return devicesOnFirmware;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareCountInfo> getAllSeenFirmwares(@Auth final AccessToken accessToken) {

        final Jedis jedis = jedisPool.getResource();
        final List<FirmwareCountInfo> firmwareCounts = Lists.newArrayList();
        try {
            final Set<Tuple> seenFirmwares = jedis.zrangeWithScores(FIRMWARES_SEEN_SET_KEY, 0, -1);
            final Pipeline pipe = jedis.pipelined();
            final Map<String, redis.clients.jedis.Response<Long>> responseMap = Maps.newHashMap();
            for (final Tuple fwInfo:seenFirmwares) {
                responseMap.put(fwInfo.getElement(), pipe.zcard(fwInfo.getElement()));
            }
            pipe.sync();
            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                final long fwCount = responseMap.get(fwVersion).get();
                final long lastSeen = (long) fwInfo.getScore();
                if (fwCount > 0) {
                    firmwareCounts.add(new FirmwareCountInfo(fwInfo.getElement(), fwCount, lastSeen));
                }
            }
        } catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch (Exception exception) {
            LOGGER.error("Failed retrieving FW list: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } finally {
            try {
                jedisPool.returnResource(jedis);
            } catch (JedisConnectionException e) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }

        return firmwareCounts;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/list_by_time")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareCountInfo> getSeenFirmwareByTime(@Auth final AccessToken accessToken,
                                                            @QueryParam("range_start") final Long rangeStart,
                                                            @QueryParam("range_end") final Long rangeEnd) {

        if(rangeStart == null) {
            LOGGER.error("Missing range_start parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if(rangeEnd == null) {
            LOGGER.error("Missing range_end parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final List<FirmwareCountInfo> firmwareCounts = Lists.newArrayList();
        try {
            final Set<Tuple> seenFirmwares = jedis.zrangeByScoreWithScores(FIRMWARES_SEEN_SET_KEY, rangeStart, rangeEnd);
            final Pipeline pipe = jedis.pipelined();
            final Map<String, redis.clients.jedis.Response<Long>> responseMap = Maps.newHashMap();
            for (final Tuple fwInfo:seenFirmwares) {
                responseMap.put(fwInfo.getElement(), pipe.zcount(fwInfo.getElement(), rangeStart, rangeEnd));
            }
            pipe.sync();
            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                final long fwCount = responseMap.get(fwVersion).get();
                final long lastSeen = (long) fwInfo.getScore();
                if (fwCount > 0) {
                    firmwareCounts.add(new FirmwareCountInfo(fwInfo.getElement(), fwCount, lastSeen));
                }
            }

        } catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch (Exception exception) {
            LOGGER.error("Failed retrieving FW list by time: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } finally {
            try {
                jedisPool.returnResource(jedis);
            } catch (JedisConnectionException e) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }

        return firmwareCounts;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/{device_id}/history")
    @Produces(MediaType.APPLICATION_JSON)
    public TreeMap<Long, String> getFirmwareHistory(@Auth final AccessToken accessToken,
                                                 @PathParam("device_id") final String deviceId) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final TreeMap<Long, String> fwHistory = Maps.newTreeMap();

        try {
            final Set<Tuple> seenFirmwares = jedis.zrangeWithScores(FIRMWARES_SEEN_SET_KEY, 0, -1);
            final Map<String, redis.clients.jedis.Response<Double>> responseMap = Maps.newHashMap();

            final Pipeline pipe = jedis.pipelined();
            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                responseMap.put(fwVersion, pipe.zscore(fwVersion, deviceId));
            }
            pipe.sync();

            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                final Double timestamp = responseMap.get(fwVersion).get();
                if(timestamp != null) {
                    fwHistory.put(timestamp.longValue(), fwVersion);
                }
            }

        } catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch (Exception exception) {
            LOGGER.error("Failed retrieving FW history for device {}: {}", deviceId, exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } finally {
            try {
                jedisPool.returnResource(jedis);
            } catch (JedisConnectionException e) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }

        return fwHistory;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/{device_id}/latest")
    @Produces(MediaType.APPLICATION_JSON)
    public FirmwareInfo getLatestFirmwareVersion(@Auth final AccessToken accessToken,
                                                @PathParam("device_id") final String deviceId) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Optional<FirmwareInfo> latestInfo = getFirmwareInfoForDevice(deviceId);
        if (!latestInfo.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return latestInfo.get();
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ, OAuthScope.ADMINISTRATION_WRITE})
    @GET
    @Timed
    @Path("/{group_name}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareInfo> getFirmwareStatusForGroup(@Auth final AccessToken accessToken,
                                            @PathParam("group_name") final String groupName) {

        if(groupName == null) {
            LOGGER.error("Missing groupName parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        //Get list of all devices in group
        final Optional<Team> team = teamStore.getTeam(groupName, TeamStore.Type.DEVICES);
        if(!team.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }



        final Team group = team.get();
        final List<String> groupIds = Lists.newArrayList(group.ids);

        final List<FirmwareInfo> firmwares = Lists.newArrayList();
        final List<List<String>> devices = Lists.partition(groupIds, SensorsViewsDynamoDB.MAX_LAST_SEEN_DEVICES);

        for (final List<String> deviceSubList : devices) {
            final Optional<List<FirmwareInfo>> fwInfo = getFirmwareInfoForDevices(Sets.newHashSet(deviceSubList));
            if (fwInfo.isPresent()) {
                firmwares.addAll(fwInfo.get());
            }
        }
        return firmwares;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Timed
    @Path("/history/{fw_version}/")
    public void clearFWHistory(@Auth final AccessToken accessToken,
                                      @PathParam("fw_version") final String fwVersion) {
        if(fwVersion == null) {
            LOGGER.error("Missing fw_version parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zrem(FIRMWARES_SEEN_SET_KEY, fwVersion) > 0) {
                jedis.del(fwVersion);
            } else {
                LOGGER.error("Attempted to delete non-existent Redis member: {}", fwVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Failed clearing fw history for {} {}.", fwVersion, e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/names/{fw_hash}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getFWNames(
            @Auth final AccessToken accessToken,
            @PathParam("fw_hash") final String fwHash) {
        return firmwareVersionMappingDAO.get(fwHash.toLowerCase());
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @POST
    @Timed
    @Path("/names")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getFWNamesBatch(
            @Auth final AccessToken accessToken,
            @Valid @NotNull final ImmutableSet<String> fwHashSet) {
        return firmwareVersionMappingDAO.getBatch(fwHashSet);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Path("/names/add_map/{fw_version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> addFWNameMap(
        @Auth final AccessToken accessToken,
        @PathParam("fw_version") final String fwVersion) {

        ObjectListing objectListing;

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
            .withBucketName("hello-firmware").withPrefix("sense/" + fwVersion);

        final List<String> keys = Lists.newArrayList();
        int i = 0;
        do {
            objectListing = s3Client.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary :
                objectListing.getObjectSummaries()) {
                if(objectSummary.getKey().contains("build_info.txt")) {
                    keys.add(objectSummary.getKey());
                }
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
            i++;
        } while (objectListing.isTruncated() && i < 5);

        for(final String key: keys) {
            final String humanVersion = key.split("/")[1];
            if (!humanVersion.equals(fwVersion)) {
                continue;
            }

            final S3Object s3Object = s3Client.getObject("hello-firmware", key);
            String text;
            try(final S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent()) {
                text = CharStreams.toString(new InputStreamReader(s3ObjectInputStream, Charsets.UTF_8));
            } catch (IOException e) {
                LOGGER.error("error=build_info_read_failure key={} message={}", key, e.getMessage());
                continue;
            }

            final Iterable<String> strings = Splitter.on("\n").split(text);
            final String firstLine = strings.iterator().next();
            final String[] parts = firstLine.split(":");
            final String hash = (parts[1].trim().length() < 6) ? Integer.toHexString(Integer.parseInt(parts[1].trim())) : parts[1].trim();

            firmwareVersionMappingDAO.put(hash, humanVersion);
            LOGGER.info("action=put_fw_map hash={} key={}", hash, key);

            return ImmutableMap.of("fw_hash", hash);
        }
        return Collections.EMPTY_MAP;
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/{device_id}/ota_history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OTAHistory> getOTAHistory(@Auth final AccessToken accessToken,
                                           @PathParam("device_id") final String deviceId,
                                           @QueryParam("range_start") final Long rangeStart,
                                           @QueryParam("range_end") final Long rangeEnd) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if(rangeStart == null) {
            LOGGER.error("Missing range_start parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if(rangeEnd == null) {
            LOGGER.error("Missing range_end parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final DateTime rangeStartDate = new DateTime(rangeStart * 1000);
        final DateTime rangeEndDate = new DateTime(rangeEnd * 1000);

        final List<OTAHistory> otaHistoryEntries = otaHistoryDAO.getOTAEvents(deviceId, rangeStartDate, rangeEndDate);

        return otaHistoryEntries;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Timed
    @Path("/{device_id}/reset_to_factory_fw")
    public void resetDeviceToFactoryFW(@Auth final AccessToken accessToken,
                                                    @PathParam("device_id") final String deviceId,
                                                    @QueryParam("fw_version") final Integer fwVersion) {
        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(fwVersion == null) {
            LOGGER.error("Missing fw_version parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        LOGGER.info("Resetting to factory FW for device: {} on FW Version: {}", deviceId, fwVersion);
        final Map<ResponseCommand, String> issuedCommands = new HashMap<>();
        issuedCommands.put(ResponseCommand.RESET_TO_FACTORY_FW, "true");
        responseCommandsDAODynamoDB.insertResponseCommands(deviceId, fwVersion, issuedCommands);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Path("/{group_name}/upgrade_nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UpgradeNodeRequest> getFWUpgradeNode(@Auth final AccessToken accessToken, @PathParam("group_name") final String groupName) {

        if(groupName == null) {
            LOGGER.error("Missing group name parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        LOGGER.info("Retrieving FW upgrade node(s) for group: {}", groupName);

        return firmwareUpgradePathDAO.getFWUpgradeNodesForGroup(groupName);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/upgrades/add_node")
    public void addFWUpgradeNode(@Auth final AccessToken accessToken, @Valid final UpgradeNodeRequest nodeRequest) {

        LOGGER.info("Adding FW upgrade node for group: {} on FW Version: {} to FW Version: {} @ {}% Rollout", nodeRequest.groupName, nodeRequest.fromFWVersion, nodeRequest.toFWVersion, nodeRequest.rolloutPercent);
        firmwareUpgradePathDAO.insertFWUpgradeNode(nodeRequest);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/upgrades/delete_node/{group_name}/{from_fw_version}")
    public void deleteFWUpgradeNode(@Auth final AccessToken accessToken,
                                    @PathParam("group_name") final String groupName,
                                    @PathParam("from_fw_version") final Integer fromFWVersion) {

        LOGGER.info("Deleting FW upgrade node for group: {} on FW Version: {}", groupName, fromFWVersion);
        firmwareUpgradePathDAO.deleteFWUpgradeNode(groupName, fromFWVersion);
    }


    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/certified_combinations")
    public Response updateCertifiedCombinations(@Auth final AccessToken accessToken,
                                         @Valid final Set<String> updatedCombinations) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            final Pipeline pipe = jedis.pipelined();
            pipe.multi();
            pipe.del(CERTIFIED_FIRMWARE_SET_KEY);
            for (final String combination : updatedCombinations) {
                pipe.sadd(CERTIFIED_FIRMWARE_SET_KEY, combination);
            }
            pipe.exec();
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
                            String.format("Failed to update certified combined firmware versions because %s", e.getMessage()))).build());
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }
        return Response.noContent().build();
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/certified_combinations")
    public Set<String> retrieveCertifiedCombination(@Auth final AccessToken accessToken) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.smembers(CERTIFIED_FIRMWARE_SET_KEY);
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
    }

    private Optional<FirmwareInfo> getFirmwareInfoForDevice(final String deviceId) {

        final Optional<List<FirmwareInfo>> fwInfo = getFirmwareInfoForDevices(Sets.newHashSet(deviceId));

        if (fwInfo.isPresent()) {
            return Optional.of(fwInfo.get().get(0));
        }

        return Optional.absent();
    }

    private Optional<List<FirmwareInfo>> getFirmwareInfoForDevices(final Set<String> deviceIds) {

        final Jedis jedis = jedisPool.getResource();

        try {

            final List<FirmwareInfo> fwInfo = Lists.newArrayList();
            final Map<String, redis.clients.jedis.Response<String>> topResponseMap = Maps.newHashMap();
            final Map<String, redis.clients.jedis.Response<String>> middleResponseMap = Maps.newHashMap();
            final Map<String, redis.clients.jedis.Response<Double>> timestampResponseMap = Maps.newHashMap();

            Pipeline pipe = jedis.pipelined();
            for (final String deviceId : deviceIds) {
                topResponseMap.put(deviceId, pipe.hget(DEVICE_KEY_BASE.concat(deviceId), "top_version"));
                middleResponseMap.put(deviceId, pipe.hget(DEVICE_KEY_BASE.concat(deviceId), "middle_version"));
            }
            pipe.sync();

            pipe = jedis.pipelined();
            for (final String deviceId : deviceIds) {
                if (!middleResponseMap.containsKey(deviceId) || middleResponseMap.get(deviceId).get() == null) {
                    continue;
                }

                final String middleFWVersion = (middleResponseMap.get(deviceId) != null) ? middleResponseMap.get(deviceId).get() : "0";
                timestampResponseMap.put(deviceId, pipe.zscore(middleFWVersion, deviceId));
            }
            pipe.sync();

            for (final String deviceId : deviceIds) {
                String topFWVersion = "0";
                if (topResponseMap.containsKey(deviceId) && topResponseMap.get(deviceId).get() != null) {
                    topFWVersion = topResponseMap.get(deviceId).get();
                }

                if (!middleResponseMap.containsKey(deviceId) || middleResponseMap.get(deviceId).get() == null) {
                    continue;
                }

                final String middleFWVersion = (middleResponseMap.get(deviceId) != null) ? middleResponseMap.get(deviceId).get() : "0";
                final Long middleFWTimestamp = (timestampResponseMap.get(deviceId) != null) ? timestampResponseMap.get(deviceId).get().longValue() : 0;

                fwInfo.add(new FirmwareInfo(middleFWVersion, topFWVersion, deviceId, middleFWTimestamp));
            }

            if (fwInfo.size() < 1) {
                return Optional.absent();
            }
            return Optional.of(fwInfo);

        } catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } catch (Exception exception) {
            LOGGER.error("Failed retrieving FW list: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
        } finally {
            try {
                jedisPool.returnResource(jedis);
            } catch (JedisConnectionException e) {
                LOGGER.error("Jedis Connection Exception while returning resource to pool. Redis server down?");
            }
        }
        return Optional.absent();
    }

}