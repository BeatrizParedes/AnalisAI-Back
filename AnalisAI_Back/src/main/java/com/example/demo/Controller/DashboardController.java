package com.example.demo.Controller;

import com.example.demo.DTO.DashboardStatsDTO;
import com.example.demo.Service.DashboardService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para expor os dados do Dashboard de Progresso (Feature 2).
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin // Permite que o frontend Angular acesse este endpoint
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Endpoint principal para o Dashboard de Progresso (MVP).
     *
     * Retorna as estatísticas de progresso do projeto (baseado no JQL padrão).
     * Cobre os Critérios de Aceitação:
     * - Gráficos (contagem) de tarefas concluídas, em andamento, em atraso.
     * - Percentual geral de progresso.
     *
     * @return Um DTO com todas as métricas calculadas.
     */
    @GetMapping("/stats")
    public DashboardStatsDTO getProjectStats() {
        return dashboardService.getProjectStats();
    }
}
