package com.example.demo.Service;

import com.example.demo.DTO.DashboardStatsDTO;
import com.example.demo.DTO.IssueSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardService {

    private final JiraClient jiraClient;

    public DashboardService(JiraClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    public DashboardStatsDTO getProjectStats() {

        // Busca todas as issues diretamente, sem precisar passar token
        List<IssueSummary> allIssues = jiraClient.fetchAllAsSummaries();

        DashboardStatsDTO stats = new DashboardStatsDTO();

        if (allIssues == null || allIssues.isEmpty()) {
            log.warn("Nenhuma issue encontrada pelo JiraClient. Retornando estatísticas zeradas.");
            stats.setTotalTasks(0);
            stats.setProgressPercentage(0.0);
            return stats;
        }

        int total = allIssues.size();
        stats.setTotalTasks(total);

        Map<String, Long> tasksByStatus = allIssues.stream()
                .collect(Collectors.groupingBy(
                        issue -> (issue.status() != null && !issue.status().isBlank())
                                ? issue.status()
                                : "Sem Status",
                        Collectors.counting()
                ));

        stats.setTasksByStatus(tasksByStatus);

        int completed = tasksByStatus.getOrDefault("Done", 0L).intValue();
        stats.setCompletedTasks(completed);
        stats.setInProgressTasks(tasksByStatus.getOrDefault("In Progress", 0L).intValue());
        stats.setTodoTasks(tasksByStatus.getOrDefault("To Do", 0L).intValue());

        LocalDate today = LocalDate.now();
        int delayed = (int) allIssues.stream().filter(issue -> {
            boolean isOverdue = false;

            if (issue.duedate() != null && !issue.duedate().isBlank()) {
                try {
                    LocalDate dueDate = LocalDate.parse(issue.duedate());
                    isOverdue = dueDate.isBefore(today);
                } catch (Exception e) {
                    log.error("Erro ao parsear duedate: {} (Key: {})",
                            issue.duedate(), issue.key(), e);
                }
            }

            boolean isCompleted = "Done".equalsIgnoreCase(issue.status());

            return isOverdue && !isCompleted;

        }).count();

        stats.setDelayedTasks(delayed);

        double percentage = (total == 0) ? 0.0 : ((double) completed / total) * 100.0;
        stats.setProgressPercentage(percentage);

        log.info("Estatísticas calculadas: Total={}, Concluídas={}, EmAtraso={}, Progresso={}%", 
                total, completed, delayed, String.format("%.2f", percentage));

        return stats;
    }
}