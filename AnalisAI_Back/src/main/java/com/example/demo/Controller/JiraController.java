package com.example.demo.Controller;

import com.example.demo.Service.JiraClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/jira")
@CrossOrigin
public class JiraController {

    private final JiraClient jira;

    public JiraController(JiraClient jira) {
        this.jira = jira;
    }

    // Endpoint de teste
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Jira Backend OK");
    }

    // Lista todos os projetos (RAW)
    @GetMapping("/projects/raw")
    public ResponseEntity<String> listProjects() {
        return ResponseEntity.ok(jira.listProjectsRaw());
    }

    // Lista todas as issues resumidas (DTO)
    @GetMapping("/issues")
    public ResponseEntity<List<?>> listSummaries() {
        List<?> issues = jira.fetchAllAsSummaries();

        if (issues == null || issues.isEmpty()) {
            return ResponseEntity.noContent().build(); // Retorna 204 se não houver issues
        }

        return ResponseEntity.ok(issues);
    }

    // Retorna todas as issues paginadas igual ao script PowerShell
    @GetMapping("/issues/raw")
    public ResponseEntity<Map<String, Object>> listIssuesRaw(
            @RequestParam(required = false) String nextPageToken
    ) {
        // Chama o JiraClient que retorna JSON como Map
        Map<String, Object> response = jira.fetchAllPagesAsMap(nextPageToken);

        if (response == null || ((List<?>) response.get("issues")).isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 se não houver issues
        }

        return ResponseEntity.ok(response);
    }
}