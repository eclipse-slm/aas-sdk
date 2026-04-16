package org.eclipse.slm.aas.clients.auth;

public class BearerTokenAuthRequestInterceptor extends AuthRequestInterceptor {

    private final String bearerToken;

    public BearerTokenAuthRequestInterceptor(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    @Override
    public String getAuthorizationHeaderValue() {
        return "Bearer " + bearerToken;
    }
}
