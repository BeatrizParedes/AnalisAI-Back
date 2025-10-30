package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueFields(
        String summary,
        JiraStatus status,
        JiraAssignee assignee,
        String updated,
        String created,
        JiraProject project,
        JiraIssueType issuetype
) {}
