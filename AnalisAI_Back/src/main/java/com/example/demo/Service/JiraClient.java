// src/main/java/com/example/demo/Service/JiraClient.java
package com.example.demo.Service;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class JiraClient {

    private static final String SEARCH_JQL_PATH    = "/rest/api/3/search/jql";
    private static final String PROJECT_SEARCH_PATH = "/rest/api/3/project/search";
    private static final String MYSELF_PATH         = "/rest/api/3/myself";

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
        String basic = Base64.getEncoder()
                .encodeToString((email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
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

    public String pingMe() {
        return webClient.get()
                .uri(MYSELF_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("erro ao chamar /myself: " + ex.getMessage()))
                .block();
    }

    public String listProjectsRaw() {
        return webClient.get()
                .uri(PROJECT_SEARCH_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /** RAW de /search/jql — envia nextPageToken opcional */
    public String searchPageRaw(String nextPageToken) {
        JiraSearchJqlRequest req = new JiraSearchJqlRequest(
                defaultJql,
                pageSize,
                List.of("summary","status","assignee","updated","created","project","issuetype"),
                (nextPageToken != null && !nextPageToken.isBlank()) ? nextPageToken : null
        );

        return webClient.post()
                .uri(SEARCH_JQL_PATH)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /** Lista resumida para o front — pagina até isLast=true */
    public List<IssueSummary> fetchAllAsSummaries() {
        String token = null;
        boolean last = false;
        List<IssueSummary> out = new ArrayList<>();

        do {
            JiraSearchJqlRequest req = new JiraSearchJqlRequest(
                    defaultJql,
                    pageSize,
                    List.of("summary","status","assignee","updated","created","project","issuetype"),
                    token
            );

            JiraSearchJqlResponse resp = webClient.post()
                    .uri(SEARCH_JQL_PATH)
                    .bodyValue(req)
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

            token = (resp != null) ? resp.nextPageToken() : null;
            last  = (resp != null && Boolean.TRUE.equals(resp.isLast()));
        } while (!last && token != null && !token.isBlank());

        return out;
    }
}
