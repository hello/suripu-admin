package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class AccessTokenAdmin extends AccessToken {

    @JsonProperty("id")
    private Long id;

    public AccessTokenAdmin(final Long id, final UUID token, final UUID refreshToken, final Long expiresIn, final DateTime createdAt, final Long accountId, final Long appId, final OAuthScope[] scopes) {
        super(token, refreshToken, expiresIn, createdAt, accountId, appId, scopes);
        this.id = id;
    }

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
}
