package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredropwizard.oauth.AccessToken;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class AccessTokenAdmin extends AccessToken {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("app_name")
    private String appName;

    @JsonIgnore
    public final String tokenType = "Bearer";

    public AccessTokenAdmin(final Long id, final UUID token, final UUID refreshToken, final Long expiresIn, final Long refreshExpiresIn, final DateTime createdAt, final Long accountId, final Long appId, final OAuthScope[] scopes, final String appName) {
        super(token, refreshToken, expiresIn, refreshExpiresIn, createdAt, accountId, appId, scopes);
        this.id = id;
        this.appName = appName;
    }

    @JsonProperty("app_id")
    public Long getAppId() { return this.appId; }

    @JsonProperty("created_at")
    public DateTime getCreatedAt() {
        return this.createdAt;
    }

    @JsonProperty("scopes")
    public List<OAuthScope> getScopes() {
        return Arrays.asList(this.scopes);
    }

    @JsonIgnore
    public String serializeAccessToken() {
        return super.serializeAccessToken();
    }

    @JsonIgnore
    public String serializeRefreshToken() {
        return super.serializeRefreshToken();
    }

    @JsonIgnore
    public String getName() {
        return super.getName();
    }
}
