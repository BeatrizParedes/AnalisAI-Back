package com.example.demo.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSearchJqlResponse(
    List<JiraIssue> issues,
    String nextPageToken,
    Boolean isLast
) {}
