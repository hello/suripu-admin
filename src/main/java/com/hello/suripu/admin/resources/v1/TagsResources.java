package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.hello.suripu.core.db.TagStoreDAODynamoDB;
import com.hello.suripu.core.models.Tag;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/tags")
public class TagsResources {

    private final TagStoreDAODynamoDB tagStore;

    public TagsResources(final TagStoreDAODynamoDB tagStore) {
        this.tagStore = tagStore;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tag> allDeviceTags(@Auth final AccessToken accessToken) {
        return tagStore.getTags(TagStoreDAODynamoDB.Type.DEVICES);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/device_tags/{device_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> allTagsForDevice(@Auth final AccessToken accessToken, @PathParam("device_id") String deviceId) {
        final List<String> deviceTags = Lists.newArrayList();
        final List<Tag> allTags = tagStore.getTags(TagStoreDAODynamoDB.Type.DEVICES);
        for (final Tag tag : allTags) {
            if (tag.ids.contains(deviceId)) {
                deviceTags.add(tag.name);
            }
        }
        return deviceTags;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tag> allUsersTags(@Auth final AccessToken accessToken) {
        return tagStore.getTags(TagStoreDAODynamoDB.Type.USERS);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/devices/{tag_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Tag getDeviceTag(@Auth final AccessToken accessToken, @PathParam("tag_name") String tagName) {

        final Optional<Tag> tag = tagStore.getTag(tagName, TagStoreDAODynamoDB.Type.DEVICES);
        if(tag.isPresent()) {
            return tag.get();
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/users/{tag_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Tag getUsersTag(@Auth final AccessToken accessToken, @PathParam("tag_name") String tagName) {
        final Optional<Tag> tag = tagStore.getTag(tagName, TagStoreDAODynamoDB.Type.USERS);
        if(tag.isPresent()) {
            return tag.get();
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @PUT
    @Path("/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createDeviceTag(@Auth final AccessToken accessToken, @Valid final Tag tag) {
        tagStore.createTag(tag, TagStoreDAODynamoDB.Type.DEVICES);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @PUT
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createUsersTag(@Auth final AccessToken accessToken, @Valid final Tag tag) {
        tagStore.createTag(tag, TagStoreDAODynamoDB.Type.USERS);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Path("/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addToDevicesTag(
            @Auth final AccessToken accessToken,
            @Valid final Tag tag) {
        tagStore.add(tag.name, TagStoreDAODynamoDB.Type.DEVICES, Lists.newArrayList(tag.ids));
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addToUsersTag(@Auth final AccessToken accessToken,
                               @Valid final Tag tag) {
        tagStore.add(tag.name, TagStoreDAODynamoDB.Type.USERS, Lists.newArrayList(tag.ids));
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Path("/devices/{tag_name}")
    public void deleteDevicesTag(
            @Auth final AccessToken accessToken,
            @PathParam("tag_name") final String tagName) {
        final Tag deleteTag = Tag.create(tagName, Sets.<String>newHashSet());
        tagStore.delete(deleteTag, TagStoreDAODynamoDB.Type.DEVICES);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Path("/users/{tag_name}")
    public void deleteUsersTag(
            @Auth final AccessToken accessToken,
            @PathParam("tag_name") final String tagName) {
        final Tag deleteTag = Tag.create(tagName, Sets.<String>newHashSet());
        tagStore.delete(deleteTag, TagStoreDAODynamoDB.Type.USERS);

    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Path("/devices/{tag_name}/{device_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeFromDevicesTag(
            @Auth final AccessToken accessToken,
            @PathParam("tag_name") final String tagName,
            @PathParam("device_id") final String deviceId){
        final List<String> ids = Lists.newArrayList();
        ids.add(deviceId);
        tagStore.remove(tagName, TagStoreDAODynamoDB.Type.DEVICES, ids);
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_WRITE})
    @DELETE
    @Path("/users/{tag_name}/{user_id}")
    public void removeFromUsersTag(
            @Auth final AccessToken accessToken,
            @PathParam("tag_name") final String tagName,
            @PathParam("user_id") final Long userId) {
        final List<String> ids = Lists.newArrayList();
        ids.add(String.valueOf(userId));
        tagStore.remove(tagName, TagStoreDAODynamoDB.Type.USERS, ids);
    }
}
