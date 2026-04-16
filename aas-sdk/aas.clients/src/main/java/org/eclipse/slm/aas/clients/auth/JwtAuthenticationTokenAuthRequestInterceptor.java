package org.eclipse.slm.aas.clients.auth;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtAuthenticationTokenAuthRequestInterceptor extends AuthRequestInterceptor {

    private final JwtAuthenticationToken jwtAuthenticationToken;

    public JwtAuthenticationTokenAuthRequestInterceptor(JwtAuthenticationToken jwtAuthenticationToken) {
        this.jwtAuthenticationToken = jwtAuthenticationToken;
    }

    @Override
    public String getAuthorizationHeaderValue() {
        return "Bearer " + jwtAuthenticationToken.getToken().getTokenValue();
    }
}
