package com.example.demo.DTO;

import java.util.List;

public record JiraSearchJqlQuery(
        String jql,
        Integer maxResults,
        List<String> fields,
        String nextPageToken
) {}
