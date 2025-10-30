package com.example.demo.Service;

import com.example.demo.Model.Tarefa;
import com.example.demo.Repository.TarefaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TarefaService {

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ProjetoService projetoService;

    // Listar todas as tarefas
    public List<Tarefa> listarTarefas() {
        return tarefaRepository.findAll();
    }

    // Criar ou atualizar uma tarefa
    public Tarefa salvarTarefa(Tarefa tarefa) {
        Tarefa novaTarefa = tarefaRepository.save(tarefa);

        // Recalcula automaticamente o prazo otimizado do projeto
        if (novaTarefa.getProjeto() != null) {
            projetoService.recalcularPrazoOtimizado(novaTarefa.getProjeto().getId());
        }

        return novaTarefa;
    }

    // Excluir uma tarefa
    public void deletarTarefa(Long id) {
        Tarefa tarefa = tarefaRepository.findById(id).orElse(null);

        if (tarefa != null && tarefa.getProjeto() != null) {
            Long projetoId = tarefa.getProjeto().getId();
            tarefaRepository.deleteById(id);

            // Recalcula após excluir a tarefa
            projetoService.recalcularPrazoOtimizado(projetoId);
        } else {
            tarefaRepository.deleteById(id);
        }
    }

    // Marcar como concluída
    public Tarefa marcarComoConcluida(Long id) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        tarefa.setConcluida(true);
        Tarefa atualizada = tarefaRepository.save(tarefa);

        // Recalcula após concluir a tarefa
        projetoService.recalcularPrazoOtimizado(tarefa.getProjeto().getId());

        return atualizada;
    }
}