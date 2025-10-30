package com.example.demo.DTO;

import java.util.List;

public record JiraSearchJqlRequest(
        String query,
        Integer maxResults,
        List<String> fields,
        String nextPageToken
) {}
