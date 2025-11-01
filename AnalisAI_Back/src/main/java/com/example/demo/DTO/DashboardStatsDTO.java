package com.example.demo.DTO;

import lombok.Data;
import java.util.Map;

/**
 * DTO (Data Transfer Object) para transportar as estatísticas do dashboard
 * para o frontend.
 * Cobre os Critérios de Aceitação da feature 2.
 */
@Data
public class DashboardStatsDTO {

    // Contagens para os gráficos
    private int totalTasks;
    private int completedTasks;      // "Done"
    private int inProgressTasks;     // "In Progress"
    private int todoTasks;           // "To Do"
    private int delayedTasks;        // Em Atraso (duedate < hoje E status != "Done")

    /**
     * Percentual geral de progresso (0.0 a 100.0)
     * (concluídas / total) * 100
     */
    private double progressPercentage;

    /**
     * Mapa flexível para o frontend poder montar gráficos customizados.
     * Ex: {"To Do": 10, "In Progress": 5, "Done": 20, "Blocked": 2}
     */
    private Map<String, Long> tasksByStatus;
}
