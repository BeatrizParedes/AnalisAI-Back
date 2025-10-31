package com.example.demo.DTO;

import java.time.Instant;

/**
 * DTO para representar o retorno de token do OAuth Atlassian.
 */
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final Integer expiresIn;
    private final Instant expiresAt;
    private final String scope;

    public TokenResponse(String accessToken, String refreshToken, String tokenType,
                         Integer expiresIn, Instant expiresAt, String scope) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.scope = scope;
    }

    // ✅ Getters
    public String getAccessToken() { return accessToken; }

    public String getRefreshToken() { return refreshToken; }

    public String getTokenType() { return tokenType; }

    public Integer getExpiresIn() { return expiresIn; }

    public Instant getExpiresAt() { return expiresAt; }

    public String getScope() { return scope; }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "accessToken='" + (accessToken != null ? "[REDACTED]" : null) + '\'' +
                ", refreshToken='" + (refreshToken != null ? "[REDACTED]" : null) + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", expiresAt=" + expiresAt +
                ", scope='" + scope + '\'' +
                '}';
    }
}