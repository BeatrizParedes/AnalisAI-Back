package com.example.demo.Service;

import com.example.demo.DTO.DashboardStatsDTO;
import com.example.demo.DTO.IssueSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço focado em calcular as métricas para o Dashboard de Progresso (Feature 2).
 * Este serviço usa o JiraClient para buscar os dados VIVOS do Jira.
 */
@Slf4j
@Service
public class DashboardService {

    private final JiraClient jiraClient;

    // Injeção de dependência via construtor
    public DashboardService(JiraClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    /**
     * Calcula as estatísticas de progresso do projeto com base no JQL padrão.
     * AGORA RECEBE O ACCESS TOKEN PARA PASSAR AO JIRA CLIENT.
     *
     * @param accessToken O token de autenticação OAuth 2.0 (Bearer).
     * @return DTO com todas as métricas para o frontend.
     */
    public DashboardStatsDTO getProjectStats(String accessToken) {
        // 1. Busca todos os resumos de issues do Jira
        // ⬇️ CORREÇÃO AQUI: Passa o accessToken
        List<IssueSummary> allIssues = jiraClient.fetchAllAsSummaries(accessToken);

        DashboardStatsDTO stats = new DashboardStatsDTO();

        if (allIssues == null || allIssues.isEmpty()) {
            log.warn("Nenhuma issue encontrada pelo JiraClient. Retornando estatísticas zeradas.");
            stats.setTotalTasks(0);
            stats.setProgressPercentage(0.0);
            return stats;
        }

        int total = allIssues.size();
        stats.setTotalTasks(total);

        // 2. Agrupa as tasks por Status (para os gráficos)
        Map<String, Long> tasksByStatus = allIssues.stream()
                .collect(Collectors.groupingBy(
                        // Garante que status nulos não quebrem o agrupamento
                        issue -> (issue.getStatus() != null && !issue.getStatus().isBlank()) ? issue.getStatus() : "Sem Status",
                        Collectors.counting()
                ));
        stats.setTasksByStatus(tasksByStatus);

        // 3. Calcula contagens específicas (Concluídas, Em Andamento, A Fazer)
        // (Usamos "Done" e "In Progress" como padrão do Jira, ajuste se seus status forem diferentes)
        int completed = tasksByStatus.getOrDefault("Done", 0L).intValue();
        stats.setCompletedTasks(completed);
        stats.setInProgressTasks(tasksByStatus.getOrDefault("In Progress", 0L).intValue());
        stats.setTodoTasks(tasksByStatus.getOrDefault("To Do", 0L).intValue());


        // 4. Calcula "Em Atraso" (Delayed)
        LocalDate today = LocalDate.now();
        int delayed = (int) allIssues.stream().filter(issue -> {
            boolean isOverdue = false;
            // Verifica se tem data de entrega
            if (issue.getDuedate() != null && !issue.getDuedate().isBlank()) {
                try {
                    // O Jira retorna 'duedate' no formato "YYYY-MM-DD"
                    LocalDate dueDate = LocalDate.parse(issue.getDuedate());
                    isOverdue = dueDate.isBefore(today);
                } catch (Exception e) {
                    log.error("Erro ao parsear duedate: {} (Key: {})", issue.getDuedate(), issue.getKey(), e);
                    // Ignora erro de parsing, não conta como atrasada
                }
            }
            // Uma task está "atrasada" se a data de entrega passou E ela AINDA NÃO FOI CONCLUÍDA
            boolean isCompleted = "Done".equalsIgnoreCase(issue.getStatus());
            return isOverdue && !isCompleted;
        }).count();
        stats.setDelayedTasks(delayed);


        // 5. Calcula o Percentual Geral de Progresso
        double percentage = (total == 0) ? 0.0 : ((double) completed / total) * 100.0;
        stats.setProgressPercentage(percentage);

        log.info("Estatísticas calculadas: Total={}, Concluídas={}, EmAtraso={}, Progresso={}%",
                total, completed, delayed, String.format("%.2f", percentage));

        return stats;
    }
}

