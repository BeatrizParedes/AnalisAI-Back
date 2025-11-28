package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Service
public class JiraAuthService {

    private static final Logger log = LoggerFactory.getLogger(JiraAuthService.class);

    /**
     * Testa se email + token estão corretos chamando o /myself do Jira.
     */
    public boolean testCredentials(String email, String apiToken) {

        String basicAuth = Base64.getEncoder()
                .encodeToString((email + ":" + apiToken).getBytes());

        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            client.get()
                    .uri("https://api.atlassian.com/ex/jira/me")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return true;

        } catch (Exception e) {
            log.error("Erro ao validar credenciais Jira: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Retorna um WebClient autenticado para chamadas futuras.
     */
    public WebClient authenticatedClient(String email, String apiToken) {
        String basicAuth = Base64.getEncoder()
                .encodeToString((email + ":" + apiToken).getBytes());

        return WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}