package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraFields(
        String summary,
        JiraStatus status,     // precisa ter pelo menos 'name'
        JiraUser assignee,     // precisa ter pelo menos 'displayName'
        String updated,        // <-- campo 'updated'
        String duedate         // ⬅️ CAMPO ADICIONADO (para calcular atrasos)
) {}
