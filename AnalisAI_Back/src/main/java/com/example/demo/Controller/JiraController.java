package com.example.demo.Controller;

import com.example.demo.DTO.IssueSummary;
import com.example.demo.Service.JiraClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // <-- importa o Map

@RestController
@RequestMapping("/api/jira")
@CrossOrigin
public class JiraController {

    private final JiraClient jira;

    public JiraController(JiraClient jira) {
        this.jira = jira;
    }

    // Lista resumida (varre todas as páginas até isLast=true)
    @GetMapping("/issues")
    public List<IssueSummary> list() {
        return jira.fetchAllAsSummaries();
    }

    // RAW via GET (debug) — usa nextPageToken como query param opcional
    //@GetMapping("/issues/raw")
    //public ResponseEntity<String> getRawIssues(@RequestParam(required = false) String nextPageToken) {
      //  String result = jira.searchPageRaw(nextPageToken); // <-- usa a instância
        //return ResponseEntity.ok(result);
    //}

    // (Opcional) RAW via POST com body { "nextPageToken": "..." }
    @PostMapping("/issues/raw")
    public ResponseEntity<String> getRawIssuesPost(@RequestBody(required = false) Map<String, String> body) {
        String token = (body != null) ? body.get("nextPageToken") : null;
        String result = jira.searchPageRaw(token); // <-- usa a instância
        return ResponseEntity.ok(result);
    }
}
