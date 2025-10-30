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

    /** ✅ Testa conexão e autenticação com Jira Cloud */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        String result = jira.pingMe();
        return ResponseEntity.ok(result);
    }

    /** ✅ Lista projetos brutos (opcional, útil pra debug) */
    @GetMapping("/projects/raw")
    public ResponseEntity<String> listProjects() {
        String result = jira.listProjectsRaw();
        return ResponseEntity.ok(result);
    }

    /** ✅ Lista resumida de issues (varre todas as páginas) */
    @GetMapping("/issues")
    public List<IssueSummary> list() {
        return jira.fetchAllAsSummaries();
    }

    /** ✅ RAW via POST com body { "nextPageToken": "..." } */
    @PostMapping("/issues/raw")
    public ResponseEntity<String> getRawIssuesPost(@RequestBody(required = false) Map<String, String> body) {
        String token = (body != null) ? body.get("nextPageToken") : null;
        String result = jira.searchPageRaw(token);
        return ResponseEntity.ok(result);
    }
}