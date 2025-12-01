package com.example.demo.Service;

import com.example.demo.DTO.IssueSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class JiraClient {

    // ==========================================================
    // CONFIG: agora o token vem do application.properties
    // ==========================================================

    @Value("${jira.username}")
    private String username;

    @Value("${jira.api-token}")
    private String apiToken;

    public String getEncodedToken() {
        String raw = username + ":" + apiToken;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    // ==========================================================
    // REST client
    // ==========================================================

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String BASE_URL = "https://apisandboxps.atlassian.net/rest/api/3";
    private static final String SEARCH_JQL_PATH = "/search";

    public JiraClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl(BASE_URL)
                .build();
    }

    // ==========================================================
    // RAW: Lista projetos
    // ==========================================================
    public String listProjectsRaw(String encodedToken) {
        return webClient.get()
                .uri("/project")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // ==========================================================
    // RAW: Executa pagina específica
    // ==========================================================
    public String searchPageRaw(String encodedToken, String nextPageToken) {

        String body = """
                {
                  "jql": "project = AP ORDER BY updated DESC",
                  "startAt": %s,
                  "maxResults": 50,
                  "fields": [
                    "summary",
                    "created",
                    "status",
                    "priority",
                    "description"
                  ]
                }
                """.formatted(nextPageToken == null ? "0" : nextPageToken);

        return webClient.post()
                .uri(SEARCH_JQL_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // ==========================================================
    // FETCH: Issues resumidas (todas as páginas)
    // ==========================================================
    public List<IssueSummary> fetchAllAsSummaries(String encodedToken) {
        List<IssueSummary> result = new ArrayList<>();

        int startAt = 0;
        int total;

        do {
            String body = """
                    {
                      "jql": "project = AP ORDER BY updated DESC",
                      "startAt": %s,
                      "maxResults": 50,
                      "fields": [
                        "summary",
                        "created",
                        "status",
                        "priority",
                        "description"
                      ]
                    }
                    """.formatted(startAt);

            String json = webClient.post()
                    .uri(SEARCH_JQL_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            try {
                JsonNode root = mapper.readTree(json);
                total = root.path("total").asInt();

                for (JsonNode issue : root.path("issues")) {
                    result.add(convertToSummary(issue));
                }

            } catch (Exception e) {
                throw new RuntimeException("Erro ao parsear JSON do Jira", e);
            }

            startAt += 50;

        } while (startAt < total);

        return result;
    }

    // ==========================================================
    // Conversão para DTO
    // ==========================================================
    private IssueSummary convertToSummary(JsonNode issue) {

        String key = issue.path("key").asText();
        JsonNode fields = issue.path("fields");

        String summary = fields.path("summary").asText();
        String status = fields.path("status").path("name").asText();

        // Assignee pode ser null
        JsonNode assigneeNode = fields.path("assignee");
        String assignee = assigneeNode.isMissingNode() || assigneeNode.isNull()
                ? null
                : assigneeNode.path("displayName").asText();

        // Project
        JsonNode projectNode = fields.path("project");
        String project = projectNode.isMissingNode() || projectNode.isNull()
                ? null
                : projectNode.path("name").asText();

        // Issue Type
        JsonNode typeNode = fields.path("issuetype");
        String issuetype = typeNode.isMissingNode() || typeNode.isNull()
                ? null
                : typeNode.path("name").asText();

        // Datas
        String created = fields.path("created").asText();
        String updated = fields.path("updated").asText();

        // Due date
        String duedate = fields.path("duedate").asText("");

        return new IssueSummary(
                key,
                summary,
                status,
                assignee,
                project,
                issuetype,
                created,
                updated,
                duedate
        );
    }

}