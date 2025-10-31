package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSearchJqlResponse(
        List<JiraIssue> issues,
        String nextPageToken,
        Boolean isLast
) {}
