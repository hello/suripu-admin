package com.hello.suripu.admin.oauth;


/**
 * Created by jnorgan on 8/12/15.
 */
public class OAuthAuthorizer implements Authorizer<AccessToken> {

    @Override
    public boolean authorize(AccessToken accessToken, String role) {
        if (role.equals("ADMIN")) {
            return true;
        }

        return true;
    }
}