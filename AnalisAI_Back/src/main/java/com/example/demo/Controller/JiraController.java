package com.example.demo.Controller;

import com.example.demo.Service.JiraClient;
import com.example.demo.DTO.IssueSummary;
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
    public String me() { return jira.pingMe(); }

    @GetMapping("/projects/raw")
    public String projects() { return jira.listProjectsRaw(); }

    /** RAW do /search/jql — aceita nextPageToken opcional para navegar páginas */
    @GetMapping("/issues/raw")
    public String raw(@RequestParam(required = false) String nextPageToken) {
        return jira.searchPageRaw(nextPageToken);
    }

    /** lista resumida para o front (MVP) */
    @GetMapping("/issues")
    public List<IssueSummary> list() {
        return jira.fetchAllAsSummaries();
    }
}
