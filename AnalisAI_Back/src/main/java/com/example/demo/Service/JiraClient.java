package com.example.demo.service;

import com.example.demo.DTO.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class JiraClient {

    // ✅ CORREÇÃO 1: Endpoint correto para POST JQL na API v3
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
            @Value("${jira.jql:ORDER BY updated DESC}") String defaultJql,
            @Value("${jira.page-size:50}") Integer pageSize
    ) {
        this.apiBaseUrl = apiBaseUrl; 
        this.defaultJql = defaultJql;
        this.pageSize = (pageSize != null ? pageSize : 50);

        System.out.println("✅ JiraClient inicializado.");
        System.out.println("🌐 Base URL Jira API: " + this.apiBaseUrl);

        this.webClientBuilder = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    System.out.println("➡️  [REQUEST] " + req.method() + " " + req.url());
                    return Mono.just(req);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                    System.out.println("⬅️  [RESPONSE] HTTP " + resp.statusCode());
                    return Mono.just(resp);
                }));
    }

    private WebClient webClientWithToken(String accessToken) {
        return webClientBuilder
                .clone()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    /** 🔍 Testa autenticação */
    public String pingMe(String accessToken) {
        System.out.println("🔍 Testando conexão com Jira Cloud via /myself ...");
        return webClientWithToken(accessToken).get()
                .uri(MYSELF_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"Erro ao chamar /myself: " + ex.getMessage() + "\"}"))
                .block();
    }

    /** 📂 Lista projetos */
    public String listProjectsRaw(String accessToken) {
        System.out.println("📂 Listando projetos...");
        return webClientWithToken(accessToken).get()
                .uri(PROJECT_SEARCH_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> Mono.just("{\"error\": \"Erro ao listar projetos: " + ex.getMessage() + "\"}"))
                .block();
    }

    /** 🔄 Busca issues com paginação (endpoint /search/jql) */
    public String searchPageRaw(String accessToken, String nextPageToken) {
        try {
            int startAt = 0;
            if (nextPageToken != null && !nextPageToken.isBlank()) {
                startAt = Integer.parseInt(nextPageToken);
            }

            // Estrutura do corpo da requisição POST para /search/jql
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jql", defaultJql);
            body.put("startAt", startAt);
            body.put("maxResults", pageSize);
            
            // ✅ CORREÇÃO 2: Garantir que 'key' esteja sempre na lista de campos
            body.put("fields", List.of("key", "summary", "status", "assignee", "updated", "created", "project", "issuetype"));

            System.out.println("📤 Enviando requisição para " + SEARCH_JQL_PATH + " → startAt=" + startAt);
            
            // ✅ CORREÇÃO 3: Serializar o Map para String JSON explicitamente
            String jsonBody = mapper.writeValueAsString(body);
            System.out.println("📦 Corpo enviado (String): " + jsonBody);

            Map<String, Object> response = webClientWithToken(accessToken).post()
                    .uri(SEARCH_JQL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody) // ⬅️ Envia a string JSON pura
                    .retrieve()
                    // Tratamento de status para erro HTTP
                    .onStatus(status -> status.value() >= 400, resp -> resp.bodyToMono(String.class)
                            .flatMap(msg -> Mono.error(new RuntimeException("Erro Jira HTTP " + resp.statusCode().value() + ": " + msg))))
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return "{\"error\": \"Resposta vazia da API do Jira\"}";
            }

            // Lógica de paginação
            List<Map<String, Object>> issues = (List<Map<String, Object>>) response.get("issues");
            Integer total = (Integer) response.getOrDefault("total", 0);
            Integer maxResults = (Integer) response.getOrDefault("maxResults", pageSize);

            boolean hasNext = (startAt + maxResults) < total;
            String nextToken = hasNext ? String.valueOf(startAt + maxResults) : null;

            Map<String, Object> paginated = new LinkedHashMap<>();
            paginated.put("page", (startAt / pageSize) + 1);
            paginated.put("pageSize", pageSize);
            paginated.put("total", total);
            paginated.put("issues", issues);
            paginated.put("hasNext", hasNext);
            paginated.put("nextPageToken", nextToken);

            return mapper.writeValueAsString(paginated);

        } catch (Exception e) {
            System.err.println("❌ Erro ao buscar issues no Jira: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /** 🧩 Busca todas as issues e retorna como lista de IssueSummary */
    public List<IssueSummary> fetchAllAsSummaries(String accessToken) {
        List<IssueSummary> summaries = new ArrayList<>();
        String nextPageToken = null;

        try {
            boolean hasNext;

            do {
                String jsonResponse = searchPageRaw(accessToken, nextPageToken);
                Map<String, Object> page = mapper.readValue(jsonResponse, Map.class);

                // Tratamento para JSON de erro
                if (page.containsKey("error")) {
                    System.err.println("❌ Erro retornado na paginação: " + page.get("error"));
                    break;
                }

                List<Map<String, Object>> issues = (List<Map<String, Object>>) page.get("issues");
                if (issues != null) {
                    for (Map<String, Object> issue : issues) {
                        summaries.add(convertToSummary(issue));
                    }
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

    /** 🔄 Converte um mapa genérico da issue em IssueSummary */
    private IssueSummary convertToSummary(Map<String, Object> issueMap) {
        try {
            String key = (String) issueMap.get("key");
            Map<String, Object> fields = (Map<String, Object>) issueMap.get("fields");

            String summary = (String) fields.getOrDefault("summary", "");
            Map<String, Object> statusObj = (Map<String, Object>) fields.get("status");
            String status = statusObj != null ? (String) statusObj.get("name") : null;

            Map<String, Object> assigneeObj = (Map<String, Object>) fields.get("assignee");
            String assignee = assigneeObj != null ? (String) assigneeObj.get("displayName") : null;
            
            // Lendo e usando os 8 campos do seu DTO atualizado
            String updated = (String) fields.get("updated");
            String created = (String) fields.get("created");

            Map<String, Object> projectObj = (Map<String, Object>) fields.get("project");
            String project = projectObj != null ? (String) projectObj.get("name") : null;

            Map<String, Object> issueTypeObj = (Map<String, Object>) fields.get("issuetype");
            String issueType = issueTypeObj != null ? (String) issueTypeObj.get("name") : null;

            // CHAMA CORRETA (8 ARGUMENTOS)
            return new IssueSummary(key, summary, status, assignee, project, issueType, created, updated); 

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao converter issue: " + e.getMessage());
            
            // CHAMA DE ERRO CORRETA (8 ARGUMENTOS)
            return new IssueSummary(
                "?",                    // key
                "Erro ao converter",    // summary
                (String) null,          // status
                (String) null,          // assignee
                (String) null,          // project
                (String) null,          // issueType
                (String) null,          // created
                (String) null           // updated
            );
        }
    }
}