package com.example.demo.Controller;

import com.example.demo.DTO.IssueSummary;
import com.example.demo.Service.JiraClient;
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

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Jira Backend OK");
    }

    @GetMapping("/projects/raw")
    public ResponseEntity<String> listProjects() {

        String token = jira.getEncodedToken();
        return ResponseEntity.ok(jira.listProjectsRaw(token));
    }

    @GetMapping("/issues")
    public ResponseEntity<List<IssueSummary>> listSummaries() {

        String token = jira.getEncodedToken();
        return ResponseEntity.ok(jira.fetchAllAsSummaries(token));
    }

    @PostMapping("/issues/raw")
    public ResponseEntity<String> issuesRaw(
            @RequestBody(required = false) String nextPageToken
    ) {
        String token = jira.getEncodedToken();
        return ResponseEntity.ok(jira.searchPageRaw(token, nextPageToken));
    }
}
