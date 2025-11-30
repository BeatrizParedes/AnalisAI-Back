package com.example.demo.Service;

import com.example.demo.DTO.IssueSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class JiraClient {

    private static final String SEARCH_JQL_PATH = "/rest/api/3/search/jql";
    private static final String PROJECT_SEARCH_PATH = "/rest/api/3/project/search";
    private static final String MYSELF_PATH = "/rest/api/3/myself";

    private final WebClient.Builder webClientBuilder;
    private final String apiBaseUrl;
    private final String defaultJql;
    private final int pageSize;
    private final ObjectMapper mapper = new ObjectMapper();

    public JiraClient(
            @Value("${jira.base-url}") String apiBaseUrl,
            @Value("${jira.jql:project = AP ORDER BY updated DESC}") String defaultJql,
            @Value("${jira.page-size:50}") Integer pageSize
    ) {
        this.apiBaseUrl = apiBaseUrl;
        this.defaultJql = (defaultJql == null || defaultJql.isBlank())
                ? "ORDER BY updated DESC"
                : defaultJql;
        this.pageSize = (pageSize != null ? pageSize : 50);

        System.out.println("✔ JiraClient inicializado.");
        System.out.println("🌐 Base URL Jira API: " + this.apiBaseUrl);

        this.webClientBuilder = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

                // logs
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    System.out.println("➡ [REQUEST] " + req.method() + " " + req.url());
                    return Mono.just(req);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                    System.out.println("⬅ [RESPONSE] HTTP " + resp.statusCode());
                    return Mono.just(resp);
                }));
    }

    private WebClient webClientWithBasicAuth(String apiToken) {
        return webClientBuilder
                .clone()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + apiToken)
                .build();
    }

    public String pingMe(String apiToken) {
        System.out.println("🔍 Testando conexão com Jira Cloud via /myself ...");
        return webClientWithBasicAuth(apiToken).get()
                .uri(MYSELF_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"Erro ao chamar /myself: " + ex.getMessage() + "\"}"))
                .block();
    }

    public String listProjectsRaw(String apiToken) {
        System.out.println("📂 Listando projetos...");
        return webClientWithBasicAuth(apiToken).get()
                .uri(PROJECT_SEARCH_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"Erro ao listar projetos: " + ex.getMessage() + "\"}"))
                .block();
    }

    @SuppressWarnings("unchecked") // Aplicado aqui para os casts de 'List<Map<String, Object>>'
    public String searchPageRaw(String apiToken, String nextPageToken) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jql", defaultJql);
            body.put("maxResults", pageSize);
            body.put("fields", List.of(
                    "key", "summary", "status", "assignee",
                    "updated", "created", "project", "issuetype", "duedate"
            ));

            if (nextPageToken != null && !nextPageToken.isBlank()) {
                body.put("nextPageToken", nextPageToken);
            }

            String jsonLogBody = mapper.writeValueAsString(body);
            System.out.println("📤 POST → " + SEARCH_JQL_PATH +
                    " | nextPageToken=" + nextPageToken);
            System.out.println("📦 Corpo enviado: " + jsonLogBody);

            Map<String, Object> response = webClientWithBasicAuth(apiToken).post()
                    .uri(SEARCH_JQL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status.value() >= 400,
                            resp -> resp.bodyToMono(String.class)
                                    .flatMap(msg -> {
                                        System.err.println("❌ Erro Jira HTTP " +
                                                resp.statusCode().value() + ": " + msg);
                                        return Mono.error(new RuntimeException(
                                                "Erro Jira HTTP " + resp.statusCode().value() + ": " + msg));
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) {
                return "{\"error\": \"Resposta vazia da API do Jira\"}";
            }

            Boolean isLast = (Boolean) response.getOrDefault("isLast", Boolean.TRUE);
            String newNextPageToken = (String) response.get("nextPageToken");

            List<Map<String, Object>> issues =
                    (List<Map<String, Object>>) response.getOrDefault("issues", Collections.emptyList());

            boolean hasNext = !Boolean.TRUE.equals(isLast) && newNextPageToken != null;

            Map<String, Object> paginated = new LinkedHashMap<>();
            paginated.put("issues", issues);
            paginated.put("hasNext", hasNext);
            paginated.put("nextPageToken", newNextPageToken);
            paginated.put("issuesCount", issues.size());
            paginated.put("isLast", isLast);

            return mapper.writeValueAsString(paginated);

        } catch (Exception e) {
            System.err.println("❌ Erro ao buscar issues: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @SuppressWarnings("unchecked") // Aplicado aqui para o cast de 'List<Map<String, Object>>'
    public List<IssueSummary> fetchAllAsSummaries(String apiToken) {
        List<IssueSummary> summaries = new ArrayList<>();
        String nextPageToken = null;

        try {
            boolean hasNext;
            do {
                String jsonResponse = searchPageRaw(apiToken, nextPageToken);

                Map<String, Object> page = mapper.readValue(
                        jsonResponse,
                        new TypeReference<Map<String, Object>>() {}
                );

                if (page.containsKey("error")) {
                    System.err.println("❌ Erro retornado na paginação: " + page.get("error"));
                    break;
                }

                List<Map<String, Object>> issues =
                        (List<Map<String, Object>>) page.getOrDefault("issues", Collections.emptyList());

                for (Map<String, Object> issue : issues) {
                    summaries.add(convertToSummary(issue));
                }

                hasNext = Boolean.TRUE.equals(page.get("hasNext"));
                nextPageToken = (String) page.get("nextPageToken");

                System.out.println("✔ Fetched " + summaries.size() + " issues até agora...");
            } while (hasNext);

        } catch (Exception e) {
            System.err.println("❌ Erro ao buscar summaries: " + e.getMessage());
        }

        return summaries;
    }

    @SuppressWarnings("unchecked") // Aplicado aqui para os casts de 'Map<String, Object>'
    private IssueSummary convertToSummary(Map<String, Object> issueMap) {
        try {
            String key = (String) issueMap.get("key");
            
            // O cast problemático
            Map<String, Object> fields = (Map<String, Object>) issueMap.get("fields");

            if (fields == null) {
                return new IssueSummary(
                        key != null ? key : "?",
                        "Sem fields na resposta",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null  // duedate
                );
            }

            String summary = (String) fields.getOrDefault("summary", "");

            // Casts de objetos aninhados que também precisam ser suprimidos
            Map<String, Object> statusObj = (Map<String, Object>) fields.get("status");
            String status = statusObj != null ? (String) statusObj.get("name") : null;

            Map<String, Object> assigneeObj = (Map<String, Object>) fields.get("assignee");
            String assignee = assigneeObj != null ? (String) assigneeObj.get("displayName") : null;

            String updated = (String) fields.get("updated");
            String created = (String) fields.get("created");

            Map<String, Object> projectObj = (Map<String, Object>) fields.get("project");
            String project = projectObj != null ? (String) projectObj.get("name") : null;

            Map<String, Object> issueTypeObj = (Map<String, Object>) fields.get("issuetype");
            String issueType = issueTypeObj != null ? (String) issueTypeObj.get("name") : null;

            // <-- EXTRAI DUEDATE (campo padrão do Jira é "duedate")
            String duedate = (String) fields.get("duedate");

            return new IssueSummary(key, summary, status, assignee, project, issueType, created, updated, duedate);

        } catch (Exception e) {
            System.err.println("⚠ Erro ao converter issue: " + e.getMessage());
            return new IssueSummary("?", "Erro ao converter", null, null, null, null, null, null, null);
        }
    }
}