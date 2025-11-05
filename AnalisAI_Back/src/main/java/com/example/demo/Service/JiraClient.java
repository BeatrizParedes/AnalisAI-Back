package com.example.demo.Service;

import com.example.demo.DTO.IssueSummary;
import com.example.demo.DTO.JiraSearchJqlRequest;
import com.example.demo.DTO.JiraSearchJqlResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JiraClient {

    // Endpoints da API REST v3 (requerem CloudID)
    private static final String API_V3_BASE = "/rest/api/3";
    private static final String SEARCH_JQL_PATH = API_V3_BASE + "/search/jql";
    private static final String PROJECT_SEARCH_PATH = API_V3_BASE + "/project/search";

    // Endpoints da API Global (não requerem CloudID)
    private static final String MYSELF_PATH = "/me"; // API global

    // O WebClient para a API Global (https://api.atlassian.com)
    private final WebClient webClientApi;

    // O WebClient para a API V3 específica do site (https://api.atlassian.com/ex/jira/{cloudId})
    private final WebClient webClientV3;

    private final String defaultJql;
    private final Integer pageSize;
    private final String cloudId; // Cloud ID é necessário para a API V3

    public JiraClient(
            @Value("${atlassian.cloud-id}") String cloudId, // ID da sua instância
            @Value("${jira.jql:ORDER BY updated DESC}") String defaultJql,
            @Value("${jira.page-size:200}") Integer pageSize
    ) {
        this.cloudId = cloudId;
        this.defaultJql = defaultJql;
        this.pageSize = pageSize;

        // Cliente para a API Global (ex: /me)
        this.webClientApi = WebClient.builder()
                .baseUrl("https://api.atlassian.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .filter(logRequest())
                .build();

        // Cliente para a API V3 específica do Cloud ID (ex: /search/jql)
        this.webClientV3 = WebClient.builder()
                .baseUrl("https://api.atlassian.com/ex/jira/" + this.cloudId)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .filter(logRequest())
                .build();
    }

    // Filtro utilitário para logar requisições
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.info(">> {} {}", req.method(), req.url());
            return Mono.just(req);
        });
    }

    /**
     * Autentica na API Global (/me)
     * AGORA ACEITA O ACCESS TOKEN COMO PARÂMETRO
     */
    public String pingMe(String accessToken) {
        return webClientApi.get()
                .uri(MYSELF_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // Usa o token OAuth
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("erro ao chamar /myself")
                .block();
    }

    /**
     * Busca projetos na API V3
     * AGORA ACEITA O ACCESS TOKEN COMO PARÂMETRO
     */
    public String listProjectsRaw(String accessToken) {
        return webClientV3.get()
                .uri(PROJECT_SEARCH_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // Usa o token OAuth
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Busca JQL (paginada) na API V3
     * AGORA ACEITA O ACCESS TOKEN COMO PARÂMETRO
     */
    public String searchPageRaw(String accessToken, String nextPageToken) {
        JiraSearchJqlRequest req = new JiraSearchJqlRequest(
                defaultJql,
                pageSize != null ? pageSize : 200,
                List.of("summary", "status", "assignee", "updated", "duedate"),
                nextPageToken
        );

        return webClientV3.post()
                .uri(SEARCH_JQL_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // Usa o token OAuth
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Busca todas as issues (com paginação interna) na API V3
     * AGORA ACEITA O ACCESS TOKEN COMO PARÂMETRO
     */
    public List<IssueSummary> fetchAllAsSummaries(String accessToken) {
        String token = null;
        boolean last = false;
        List<IssueSummary> out = new ArrayList<>();

        do {
            JiraSearchJqlRequest req = new JiraSearchJqlRequest(
                    defaultJql,
                    pageSize != null ? pageSize : 200,
                    List.of("summary", "status", "assignee", "updated", "duedate"),
                    token
            );

            JiraSearchJqlResponse resp = webClientV3.post()
                    .uri(SEARCH_JQL_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // Usa o token OAuth
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
                            f != null ? f.updated() : null,
                            f != null ? f.duedate() : null
                    ));
                });
            }

            token = (resp != null) ? resp.nextPageToken() : null;
            last = (resp != null) && Boolean.TRUE.equals(resp.isLast());
        } while (!last && token != null && !token.isBlank());

        return out;
    }
}

