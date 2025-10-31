package com.example.demo.Service;

import com.example.demo.Model.Projeto;
import com.example.demo.Model.Tarefa;
import com.example.demo.Model.Tarefa;
import com.example.demo.Repository.ProjetoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjetoService {

    @Autowired
    private ProjetoRepository projetoRepository;

    // Criar novo projeto
    public Projeto salvarProjeto(Projeto projeto) {
        return projetoRepository.save(projeto);
    }

    // Listar todos os projetos
    public List<Projeto> listarProjetos() {
        return projetoRepository.findAll();
    }

    // Buscar projeto pelo ID
    public Projeto buscarPorId(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado com ID: " + id));
    }

    // Atualizar projeto e recalcular o prazo otimizado
    public Projeto atualizarProjeto(Long id, Projeto projetoAtualizado) {
        Projeto existente = buscarPorId(id);

        // Atualiza apenas os campos enviados
        if (projetoAtualizado.getNome() != null) {
            existente.setNome(projetoAtualizado.getNome());
        }

        if (projetoAtualizado.getTarefas() != null && !projetoAtualizado.getTarefas().isEmpty()) {
            existente.setTarefas(projetoAtualizado.getTarefas());
        }

        // Salva antes de recalcular para evitar inconsistências
        projetoRepository.save(existente);

        // Recalcula automaticamente após atualização
        return recalcularPrazoOtimizado(id);
    }

    // Recalcular o prazo otimizado do projeto com base nas tarefas
    public Projeto recalcularPrazoOtimizado(Long id) {
        Projeto projeto = buscarPorId(id);

        if (projeto.getTarefas() == null || projeto.getTarefas().isEmpty()) {
            projeto.setPrazoOtimizado(LocalDate.now());
            return projetoRepository.save(projeto);
        }

        // Soma as durações das tarefas
        int totalDias = projeto.getTarefas().stream()
                .mapToInt(Tarefa::getDuracaoDias)
                .sum();

        // Ajuste adicional se houver gargalos
        boolean temGargalo = projeto.getTarefas().stream().anyMatch(Tarefa::isGargalo);
        if (temGargalo) {
            totalDias += 3; // penalidade
        }

        // Define o novo prazo
        LocalDate prazo = LocalDate.now().plusDays(totalDias);
        projeto.setPrazoOtimizado(prazo);

        return projetoRepository.save(projeto);
    }
}
