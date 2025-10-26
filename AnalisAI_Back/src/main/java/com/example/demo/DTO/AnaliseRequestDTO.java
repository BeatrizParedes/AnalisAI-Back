package com.example.demo.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnaliseRequestDTO {

    /**
     * O JQL que o usuário deseja usar para a análise.
     * Se for nulo ou vazio, o backend deve usar o JQL padrão
     * definido em 'jira.jql' no application.properties.
     */
    private String jqlQuery;

    // Futuramente, pode incluir outros parâmetros, como "nomeDaAnalise", "tipoDeAnalise", etc.
}