// src/main/java/com/example/demo/DTO/JiraIssueType.java
package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueType(String name) {}
