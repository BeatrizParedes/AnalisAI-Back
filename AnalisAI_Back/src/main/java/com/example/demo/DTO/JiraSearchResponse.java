// com/example/demo/DTO/JiraSearchResponse.java
package com.example.demo.DTO;

import java.util.List;

public record JiraSearchResponse(
        Integer startAt,
        Integer maxResults,
        Integer total,
        List<JiraIssue> issues
) {
    public record JiraIssue(String key, Fields fields) {}

    public record Fields(
            String summary,
            Status status,
            Assignee assignee,
            String updated,    // pode trocar para Instant se preferir
            String created     // idem
    ) {}

    public record Status(String name) {}

    public record Assignee(String displayName) {}
}
