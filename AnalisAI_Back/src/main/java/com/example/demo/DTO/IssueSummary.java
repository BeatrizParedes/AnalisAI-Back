// src/main/java/com/example/demo/DTO/IssueSummary.java
package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IssueSummary {
    private String key;
    private String summary;
    private String status;       // ex.: "To Do", "In Progress", "Done"
    private String assignee;     // ex.: "Fulano"
    private String project;      // NOVO: Projeto da Issue
    private String issueType;    // NOVO: Tipo de Issue (Task, Bug, Story)
    private String created;      // NOVO: ISO string do created
    private String updated;      // ISO string do updated
}