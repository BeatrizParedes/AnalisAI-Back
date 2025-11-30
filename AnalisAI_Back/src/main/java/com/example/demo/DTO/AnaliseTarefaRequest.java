package com.example.demo.DTO;

import java.util.List;

public record AnaliseTarefaRequest(
    
    String issuetype,
    String created,
    String updated,
    String assignee,
    String key,             
    String summary,         // Resumo da Tarefa
    String status,                
    
    List<String> tarefasRelacionadas
) {}