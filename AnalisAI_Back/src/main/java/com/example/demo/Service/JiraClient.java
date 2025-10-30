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

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JiraClient {

    private static final String SEARCH_JQL_PATH = "/rest/api/3/search";
    private static final String PROJECT_SEARCH_PATH = "/rest/api/3/project/search";
    private static final String MYSELF_PATH = "/rest/api/3/myself";

    private final WebClient webClient;
    private final String defaultJql;
    private final int pageSize;

    public JiraClient(
            @Value("${jira.base-url}") String baseUrl,
            @Value("${jira.email}") String email,
            @Value("${jira.api-token}") String apiToken,
            @Value("${jira.jql:ORDER BY updated DESC}") String defaultJql,
            @Value("${jira.page-size:50}") Integer pageSize
    ) {
        String auth = Base64.getEncoder()
                .encodeToString((email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth)
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
                }))
                .build();

        this.defaultJql = defaultJql;
        this.pageSize = (pageSize != null ? pageSize : 50);
    }

    /** ✅ Testa autenticação básica */
    public String pingMe() {
        return webClient.get()
                .uri(MYSELF_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("erro ao chamar /myself: " + ex.getMessage()))
                .block();
    }

    /** ✅ Lista projetos */
    public String listProjectsRaw() {
        return webClient.get()
                .uri(PROJECT_SEARCH_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /** ✅ Busca issues (RAW) — formato correto Jira Cloud */
    public String searchPageRaw(String nextPageToken) {
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

            System.out.println("📤 Corpo enviado ao Jira Cloud: " + body);

            String response = webClient.post()
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

    /** ✅ Lista resumida com paginação */
    public List<IssueSummary> fetchAllAsSummaries() {
        int startAt = 0;
        boolean last = false;
        List<IssueSummary> out = new ArrayList<>();

        do {
            Map<String, Object> body = new HashMap<>();
            body.put("jql", defaultJql);
            body.put("startAt", startAt);
            body.put("maxResults", pageSize);
            body.put("fields", List.of("summary", "status", "assignee", "updated", "created", "project", "issuetype"));

            JiraSearchJqlResponse resp = webClient.post()
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