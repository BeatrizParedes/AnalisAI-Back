package com.example.demo.service.dto;

import java.util.List;

public record JiraSearchJqlRequest(
    String jql,
    Integer maxResults,
    List<String> fields,
    String nextPageToken
) {}
