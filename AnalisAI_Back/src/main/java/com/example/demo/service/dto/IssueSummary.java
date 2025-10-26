// src/main/java/com/example/demo/service/dto/IssueSummary.java
package com.example.demo.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IssueSummary {
    private String key;
    private String summary;
    private String status;         // ex.: "To Do", "In Progress", "Done"
    private String assignee;       // ex.: "Fulano"
    private String updated;        // ISO string do updated
}
