package com.example.demo.controller;

import com.example.demo.DTO.IssueSummary;
import com.example.demo.service.JiraClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jira")
@CrossOrigin
public class JiraController {

    private final JiraClient jira;

    public JiraController(JiraClient jira) {
        this.jira = jira;
    }

    // -------------------------------
    // Teste backend
    // -------------------------------
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Jira Backend OK");
    }

    // -------------------------------
    // Lista projetos
    // -------------------------------
    @GetMapping("/projects/raw")
    public ResponseEntity<String> listProjects(
            @RequestHeader(value = "Authorization", required = true) String header
    ) {
        String token = extractTokenOrThrow(header);
        return ResponseEntity.ok(jira.listProjectsRaw(token));
    }

    // -------------------------------
    // Issues resumidas
    // -------------------------------
    @GetMapping("/issues")
    public ResponseEntity<List<IssueSummary>> listSummaries(
            @RequestHeader(value = "Authorization", required = true) String header
    ) {
        String token = extractTokenOrThrow(header);
        return ResponseEntity.ok(jira.fetchAllAsSummaries(token));
    }

    // -------------------------------
    // Issues RAW + paginação
    // -------------------------------
    @PostMapping("/issues/raw")
    public ResponseEntity<String> issuesRaw(
            @RequestHeader(value = "Authorization", required = true) String header,
            @RequestBody(required = false) String nextPageToken
    ) {
        String token = extractTokenOrThrow(header);
        return ResponseEntity.ok(jira.searchPageRaw(token, nextPageToken));
    }

    // -------------------------------
    // Extrai token Basic do header
    // -------------------------------
    private String extractTokenOrThrow(String header) {
        if (header == null || !header.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                    "Authorization inválido. Envie: Authorization: Basic <token>"
            );
        }
        return header.substring(6); // remove "Basic "
    }
}
