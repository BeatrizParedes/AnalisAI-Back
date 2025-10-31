package com.example.demo.Repository;

import com.example.demo.Model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    <Tarefa> List<Tarefa> findByProjetoId(Long projetoId);
}