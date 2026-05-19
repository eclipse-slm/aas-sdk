package org.eclipse.slm.aas.clients.auth;

import org.apache.logging.log4j.util.Base64Util;

public class BasicAuthRequestInterceptor extends AuthRequestInterceptor {

    private final String username;

    private final String password;

    public BasicAuthRequestInterceptor(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getAuthorizationHeaderValue() {
        var authString = this.username + ":" + this.password;
        return "Basic " + Base64Util.encode(authString);
    }
}
