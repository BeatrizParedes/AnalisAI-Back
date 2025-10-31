package com.example.demo.controller;

import com.example.demo.DTO.IssueSummary;
import com.example.demo.service.JiraClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jira")
@CrossOrigin
public class JiraController {

    private final JiraClient jira;

    public JiraController(JiraClient jira) {
        this.jira = jira;
    }

    /**
     * Testa conexão e autenticação com Jira Cloud.
     * Requer cabeçalho Authorization: Bearer <access_token>
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping(@RequestHeader("Authorization") String authorization) {
        String accessToken = extractToken(authorization);
        String result = jira.pingMe(accessToken);
        return ResponseEntity.ok(result);
    }

    /**
     * Lista projetos (bruto).
     * Requer cabeçalho Authorization: Bearer <access_token>
     */
    @GetMapping("/projects/raw")
    public ResponseEntity<String> listProjects(@RequestHeader("Authorization") String authorization) {
        String accessToken = extractToken(authorization);
        String result = jira.listProjectsRaw(accessToken);
        return ResponseEntity.ok(result);
    }

    /**
     * Lista resumida de issues (sem paginação explícita) — 
     * Requer cabeçalho Authorization: Bearer <access_token>
     */
    @GetMapping("/issues")
    public ResponseEntity<List<IssueSummary>> listSummaries(@RequestHeader("Authorization") String authorization) {
        String accessToken = extractToken(authorization);
        List<IssueSummary> summaries = jira.fetchAllAsSummaries(accessToken);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Busca RAW de issues paginadas — endpoint POST.
     * Body opcional: { "nextPageToken": "50" }
     * Requer cabeçalho Authorization: Bearer <access_token>
     */
    @PostMapping("/issues/raw")
    public ResponseEntity<String> getRawIssuesPost(
            @RequestHeader("Authorization") String authorization,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String accessToken = extractToken(authorization);
        String nextPageToken = (body != null ? body.get("nextPageToken") : null);
        String result = jira.searchPageRaw(accessToken, nextPageToken);
        return ResponseEntity.ok(result);
    }

    /**
     * Extrai o token do header Authorization.
     * Exemplo: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Cabeçalho Authorization inválido ou ausente");
        }
        return authorizationHeader.substring(7);
    }
}