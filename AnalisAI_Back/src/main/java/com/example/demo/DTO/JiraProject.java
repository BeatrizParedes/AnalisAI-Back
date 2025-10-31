// src/main/java/com/example/demo/DTO/JiraProject.java
package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraProject(String key, String name) {}
