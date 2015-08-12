package com.hello.suripu.admin.oauth;

import com.google.common.base.Optional;
import com.hello.suripu.admin.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.ClientCredentials;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import javax.swing.text.html.Option;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthAuthenticator implements Authenticator<String, AccessToken> {

    private PersistentAccessTokenStore tokenStore;
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthAuthenticator.class);

    public OAuthAuthenticator(PersistentAccessTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Optional<AccessToken> authenticate(String credentials) throws AuthenticationException {

        final Optional<AccessToken> token = tokenStore.getAccessTokenByToken(credentials);

        if(!token.isPresent()) {
            LOGGER.warn("Token {} was not present in OAuthAuthenticator", credentials);
        }
        LOGGER.debug("Credentials: {}", credentials);
        return token;
    }
}
