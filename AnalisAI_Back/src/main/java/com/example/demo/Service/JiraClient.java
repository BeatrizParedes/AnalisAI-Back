package com.example.demo.Service;

import com.example.demo.DTO.IssueSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class JiraClient {

    // ============================
    // Paths Jira API
    // ============================
    private static final String SEARCH_JQL_PATH = "/rest/api/3/search/jql";
    private static final String PROJECT_SEARCH_PATH = "/rest/api/3/project/search";
    private static final String MYSELF_PATH = "/rest/api/3/myself";

    private final WebClient.Builder webClientBuilder;
    private final String apiBaseUrl;
    private final String defaultJql;
    private final int pageSize;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String encodedToken;

    // ============================
    // Construtor
    // ============================
    public JiraClient(
            @Value("${jira.base-url}") String apiBaseUrl,
            @Value("${jira.username}") String username,
            @Value("${jira.api-token}") String apiToken,
            @Value("${jira.jql:ORDER BY updated DESC}") String defaultJql,
            @Value("${jira.page-size:50}") Integer pageSize
    ) {
        this.apiBaseUrl = apiBaseUrl;
        this.defaultJql = (defaultJql == null || defaultJql.isBlank()) ? "project = AP ORDER BY updated DESC" : defaultJql;
        this.pageSize = (pageSize != null ? pageSize : 50);

        this.encodedToken = encodeToken(username, apiToken);

        System.out.println("✅ JiraClient inicializado.");
        System.out.println("🌐 Base URL Jira API: " + this.apiBaseUrl);

        this.webClientBuilder = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    System.out.println("➡️ [REQUEST] " + req.method() + " " + req.url());
                    return Mono.just(req);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                    System.out.println("⬅️ [RESPONSE] HTTP " + resp.statusCode());
                    return Mono.just(resp);
                }));
    }

    // ============================
    // Gera token Base64 para Basic Auth
    // ============================
    private String encodeToken(String username, String apiToken) {
        String raw = username + ":" + apiToken;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    // ============================
    // WebClient com Basic Auth
    // ============================
    private WebClient webClientWithBasicAuth() {
        return webClientBuilder
                .clone()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedToken)
                .build();
    }

    // ============================
    // Teste de autenticação /myself
    // ============================
    public String pingMe() {
        System.out.println("🔍 Testando conexão com Jira Cloud via /myself ...");
        return webClientWithBasicAuth().get()
                .uri(MYSELF_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"Erro ao chamar /myself: " + ex.getMessage() + "\"}"))
                .block();
    }

    // ============================
    // Lista projetos
    // ============================
    public String listProjectsRaw() {
        System.out.println("📂 Listando projetos...");
        return webClientWithBasicAuth().get()
                .uri(PROJECT_SEARCH_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"Erro ao listar projetos: " + ex.getMessage() + "\"}"))
                .block();
    }

    // ============================
    // Busca paginada /search/jql (retorna JSON como String)
    // ============================
    public String searchPageRaw(String nextPageToken) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jql", defaultJql);
            body.put("maxResults", pageSize);
            body.put("fields", List.of(
                    "key",
                    "summary",
                    "status",
                    "assignee",
                    "updated",
                    "created",
                    "project",
                    "issuetype",
                    "duedate"
            ));
            if (nextPageToken != null && !nextPageToken.isBlank()) {
                body.put("nextPageToken", nextPageToken);
            }

            String jsonLogBody = mapper.writeValueAsString(body);
            System.out.println("📤 Enviando requisição para " + SEARCH_JQL_PATH +
                    " → nextPageToken=" + nextPageToken);
            System.out.println("📦 Corpo enviado: " + jsonLogBody);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientWithBasicAuth().post()
                    .uri(SEARCH_JQL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status.value() >= 400,
                            resp -> resp.bodyToMono(String.class)
                                    .flatMap(msg -> {
                                        System.err.println("❌ Erro Jira HTTP " + resp.statusCode().value()
                                                + ": " + msg);
                                        return Mono.error(new RuntimeException(
                                                "Erro Jira HTTP " + resp.statusCode().value() + ": " + msg));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return "{\"error\": \"Resposta vazia da API do Jira\"}";

            Boolean isLast = (Boolean) response.getOrDefault("isLast", Boolean.TRUE);
            String newNextPageToken = (String) response.get("nextPageToken");

            @SuppressWarnings("unchecked")
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
            System.err.println("❌ Erro ao buscar issues no Jira: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ============================
    // Retorna issues paginadas como Map (para controller /issues/raw)
    // ============================
    public Map<String, Object> fetchAllPagesAsMap(String nextPageToken) {
        try {
            String jsonResponse = searchPageRaw(nextPageToken);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = mapper.readValue(jsonResponse, Map.class);

            if (responseMap.containsKey("error")) {
                System.err.println("❌ Erro retornado na paginação: " + responseMap.get("error"));
                return null;
            }

            return responseMap;
        } catch (Exception e) {
            System.err.println("❌ Erro ao converter JSON para Map: " + e.getMessage());
            return null;
        }
    }

    // ============================
    // Busca todas as issues e converte para IssueSummary
    // ============================
    public List<IssueSummary> fetchAllAsSummaries() {
        List<IssueSummary> summaries = new ArrayList<>();
        String nextPageToken = null;

        try {
            boolean hasNext;
            do {
                String jsonResponse = searchPageRaw(nextPageToken);

                @SuppressWarnings("unchecked")
                Map<String, Object> page = mapper.readValue(jsonResponse, Map.class);

                if (page.containsKey("error")) {
                    System.err.println("❌ Erro retornado na paginação: " + page.get("error"));
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> issues =
                        (List<Map<String, Object>>) page.getOrDefault("issues", Collections.emptyList());

                for (Map<String, Object> issue : issues) {
                    summaries.add(convertToSummary(issue));
                }

                hasNext = Boolean.TRUE.equals(page.get("hasNext"));
                nextPageToken = (String) page.get("nextPageToken");

                System.out.println("✅ Fetched " + summaries.size() + " issues até agora...");
            } while (hasNext);

        } catch (Exception e) {
            System.err.println("❌ Erro ao buscar summaries: " + e.getMessage());
        }

        return summaries;
    }

    // ============================
    // Conversão Map → IssueSummary
    // ============================
    @SuppressWarnings("unchecked")
    private IssueSummary convertToSummary(Map<String, Object> issueMap) {
        try {
            String key = (String) issueMap.get("key");
            Map<String, Object> fields = (Map<String, Object>) issueMap.get("fields");

            if (fields == null) {
                return new IssueSummary(key != null ? key : "?",
                        "Sem fields na resposta", null, null, null, null, null, null, null);
            }

            String summary = (String) fields.getOrDefault("summary", "");

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

            String duedate = fields.get("duedate") != null ? (String) fields.get("duedate") : null;

            return new IssueSummary(key, summary, status, assignee, project, issueType, created, updated, duedate);

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao converter issue: " + e.getMessage());
            return new IssueSummary("?", "Erro ao converter", null, null, null, null, null, null, null);
        }
    }
}