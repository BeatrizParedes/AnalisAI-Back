// src/main/java/com/example/demo/service/JiraClient.java
package com.example.demo.service;

import com.example.demo.DTO.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class JiraClient {

    private static final String SEARCH_JQL_PATH = "/rest/api/3/search";
    private static final String PROJECT_SEARCH_PATH = "/rest/api/3/project/search";
    private static final String MYSELF_PATH = "/rest/api/3/myself";

    private final WebClient.Builder webClientBuilder;
    private final String cloudId;
    private final String apiBaseUrl;
    private final String defaultJql;
    private final int pageSize;

    public JiraClient(
            @Value("${ATLASSIAN_CLOUD_ID}") String cloudId,
            @Value("${jira.jql:ORDER BY updated DESC}") String defaultJql,
            @Value("${jira.page-size:50}") Integer pageSize
    ) {
        this.cloudId = cloudId;
        this.apiBaseUrl = "https://api.atlassian.com/ex/jira/" + cloudId;
        this.defaultJql = defaultJql;
        this.pageSize = (pageSize != null ? pageSize : 50);

        this.webClientBuilder = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    System.out.println(">> " + req.method() + " " + req.url());
                    return Mono.just(req);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                    System.out.println("<< " + resp.statusCode());
                    return Mono.just(resp);
                }));
    }

    /**
     * Cria um WebClient com o access token atual do usuário.
     */
    private WebClient webClientWithToken(String accessToken) {
        return webClientBuilder
                .clone()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    /** ✅ Testa autenticação usando /myself */
    public String pingMe(String accessToken) {
        return webClientWithToken(accessToken).get()
                .uri(MYSELF_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("erro ao chamar /myself: " + ex.getMessage()))
                .block();
    }

    /** ✅ Lista projetos (usando OAuth) */
    public String listProjectsRaw(String accessToken) {
        return webClientWithToken(accessToken).get()
                .uri(PROJECT_SEARCH_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /** ✅ Busca issues (RAW) — com JQL e paginação */
    public String searchPageRaw(String accessToken, String nextPageToken) {
        try {
            int startAt = 0;
            if (nextPageToken != null && !nextPageToken.isBlank()) {
                try {
                    startAt = Integer.parseInt(nextPageToken);
                } catch (NumberFormatException ignored) {}
            }

            Map<String, Object> body = new HashMap<>();
            body.put("jql", defaultJql);
            body.put("startAt", startAt);
            body.put("maxResults", pageSize);
            body.put("fields", List.of("summary", "status", "assignee", "updated", "created", "project", "issuetype"));

            System.out.println("📤 Corpo enviado ao Jira Cloud (OAuth): " + body);

            String response = webClientWithToken(accessToken).post()
                    .uri(SEARCH_JQL_PATH)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, resp -> {
                        System.err.println("❌ Jira retornou erro HTTP: " + resp.statusCode());
                        return resp.bodyToMono(String.class)
                                .flatMap(msg -> Mono.error(new RuntimeException("Erro Jira: " + msg)));
                    })
                    .bodyToMono(String.class)
                    .block();

            System.out.println("📦 Jira response (raw): " + response);
            return response;

        } catch (Exception e) {
            System.err.println("❌ Erro ao buscar issues no Jira: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /** ✅ Busca resumida com paginação */
    public List<IssueSummary> fetchAllAsSummaries(String accessToken) {
        int startAt = 0;
        boolean last = false;
        List<IssueSummary> out = new ArrayList<>();

        do {
            Map<String, Object> body = new HashMap<>();
            body.put("jql", defaultJql);
            body.put("startAt", startAt);
            body.put("maxResults", pageSize);
            body.put("fields", List.of("summary", "status", "assignee", "updated", "created", "project", "issuetype"));

            JiraSearchJqlResponse resp = webClientWithToken(accessToken).post()
                    .uri(SEARCH_JQL_PATH)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JiraSearchJqlResponse.class)
                    .block();

            if (resp != null && resp.issues() != null) {
                resp.issues().forEach(i -> {
                    var f = i.fields();
                    out.add(new IssueSummary(
                            i.key(),
                            f != null ? f.summary() : null,
                            f != null && f.status() != null ? f.status().name() : null,
                            f != null && f.assignee() != null ? f.assignee().displayName() : null,
                            f != null ? f.updated() : null
                    ));
                });
            }

            startAt += pageSize;
            last = (resp == null || resp.issues() == null || resp.issues().isEmpty());

        } while (!last);

        return out;
    }
}