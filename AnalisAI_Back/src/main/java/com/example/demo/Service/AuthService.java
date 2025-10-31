package com.example.demo.Service;

import com.example.demo.DTO.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

/**
 * Serviço responsável pelo fluxo OAuth 2.0 da Atlassian (Jira Cloud)
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String AUTH_BASE = "https://auth.atlassian.com";
    private static final String AUTH_TOKEN = AUTH_BASE + "/oauth/token";
    private static final String ACCESSIBLE_RESOURCES = "https://api.atlassian.com/oauth/token/accessible-resources";

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public AuthService(
            @Value("${atlassian.client-id}") String clientId,
            @Value("${atlassian.client-secret}") String clientSecret,
            @Value("${atlassian.redirect-uri:http://localhost:8081/auth/callback}") String redirectUri,
            @Value("${atlassian.scope:read:jira-user read:jira-work write:jira-work manage:jira-project offline_access}") String scope
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;

        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 🔹 Gera a URL de autorização (login Atlassian)
     */
    public String buildAuthorizationUrl(String clientId, String redirectUri, String scope) {
        String state = UUID.randomUUID().toString(); // Valor anti-CSRF

        log.info("Gerando URL de autorização Atlassian com redirect_uri={}", redirectUri);

        return UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("auth.atlassian.com")
                .path("/authorize")
                .queryParam("audience", "api.atlassian.com")
                .queryParam("client_id", clientId)
                .queryParam("scope", scope)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    /**
     * 🔹 Troca o authorization code recebido por um access token + refresh token
     */
    public TokenResponse exchangeCodeForToken(String code) {
        log.info("Trocando authorization code por token...");

        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        Map<String, Object> resp = webClient.post()
                .uri(AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Erro Atlassian ao trocar code: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro Atlassian: " + errorBody));
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return toTokenResponse(resp);
    }

    /**
     * 🔹 Atualiza o access token usando o refresh token
     */
    public TokenResponse refreshAccessToken(String refreshToken) {
        log.info("Atualizando access token via refresh token...");

        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "refresh_token");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("refresh_token", refreshToken);

        Map<String, Object> resp = webClient.post()
                .uri(AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Erro Atlassian ao atualizar token: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro Atlassian: " + errorBody));
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return toTokenResponse(resp);
    }

    /**
     * 🔹 Retorna os recursos Jira acessíveis para o token (útil pra obter o cloudId)
     */
    public List<Map<String, Object>> getAccessibleResources(String accessToken) {
        log.info("Buscando accessible-resources com token atual...");

        return webClient.get()
                .uri(ACCESSIBLE_RESOURCES)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Erro Atlassian ao buscar accessible-resources: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro Atlassian: " + errorBody));
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();
    }

    // 🔹 Converte o body da resposta em um TokenResponse DTO
    private TokenResponse toTokenResponse(Map<String, Object> resp) {
        if (resp == null) {
            throw new RuntimeException("Resposta nula ao obter token");
        }

        String accessToken = asString(resp.get("access_token"));
        String refreshToken = asString(resp.get("refresh_token"));
        String tokenType = asString(resp.get("token_type"));
        Integer expiresIn = Optional.ofNullable(resp.get("expires_in"))
                .map(v -> Integer.parseInt(String.valueOf(v)))
                .orElse(null);
        String scopeResp = asString(resp.get("scope"));

        Instant expiresAt = (expiresIn != null) ? Instant.now().plusSeconds(expiresIn) : null;

        return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn, expiresAt, scopeResp);
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}