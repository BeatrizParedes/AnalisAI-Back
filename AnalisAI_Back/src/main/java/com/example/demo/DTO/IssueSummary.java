package com.example.demo.DTO;

/**
 * DTO (Data Transfer Object) para resumir as informações essenciais de uma Issue do Jira.
 * O recurso 'record' exige Java 16 ou superior.
 * Campos mapeiam os dados que são solicitados e recebidos da API do Jira.
 */
public record IssueSummary(
    String key,
    String summary,
    String status,
    String assignee,
    String project,
    String issueType,
    String created,
    String updated
) {
    // O construtor canônico (IssueSummary { ... }) é gerado automaticamente,
    // mas mantemos o toString para facilitar o debug.
    
    @Override
    public String toString() {
        return "IssueSummary{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                ", status='" + status + '\'' +
                ", project='" + project + '\'' +
                '}';
    }
}