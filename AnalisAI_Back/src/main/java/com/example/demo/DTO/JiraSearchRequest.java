// com/example/demo/DTO/JiraSearchRequest.java
package com.example.demo.DTO;

import java.util.List;

public record JiraSearchRequest(
        String jql,
        Integer startAt,
        Integer maxResults,
        List<String> fields
) {}
