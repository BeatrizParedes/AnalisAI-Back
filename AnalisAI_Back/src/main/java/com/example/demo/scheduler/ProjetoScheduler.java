package com.example.demo.scheduler;

import com.example.demo.model.Projeto;
import com.example.demo.service.ProjetoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjetoScheduler {

    @Autowired
    private ProjetoService projetoService;

    // Roda automaticamente todo dia às 2h da manhã
    @Scheduled(cron = "0 0 2 * * *")
    public void recalcularProjetosAutomaticamente() {
        System.out.println("🔄 Recalculando prazos otimizados dos projetos...");

        List<Projeto> projetos = projetoService.listarProjetos();

        for (Projeto projeto : projetos) {
            try {
                projetoService.recalcularPrazoOtimizado(projeto.getId());
                System.out.println("✅ Projeto " + projeto.getNome() + " recalculado com sucesso.");
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao recalcular projeto " + projeto.getNome() + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Recalculo automático concluído.");
    }
}