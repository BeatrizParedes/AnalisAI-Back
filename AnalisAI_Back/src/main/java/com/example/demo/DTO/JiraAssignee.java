// src/main/java/com/example/demo/DTO/JiraAssignee.java
package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraAssignee(String displayName) {}
