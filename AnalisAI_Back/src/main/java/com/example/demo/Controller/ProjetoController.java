package com.example.demo.controller;

import com.example.demo.model.Projeto;
import com.example.demo.service.ProjetoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projetos")
public class ProjetoController {

    @Autowired
    private ProjetoService projetoService;

    // Criar um novo projeto
    @PostMapping
    public Projeto criarProjeto(@RequestBody Projeto projeto) {
        return projetoService.salvarProjeto(projeto);
    }

    // Listar todos os projetos
    @GetMapping
    public List<Projeto> listarProjetos() {
        return projetoService.listarProjetos();
    }

    // Buscar um projeto específico pelo ID
    @GetMapping("/{id}")
    public Projeto buscarPorId(@PathVariable Long id) {
        return projetoService.buscarPorId(id);
    }

    // Atualizar um projeto existente (recalcula automaticamente o prazo otimizado)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizarProjeto(@PathVariable Long id, @RequestBody Projeto projetoAtualizado) {
        Projeto projeto = projetoService.atualizarProjeto(id, projetoAtualizado);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("mensagem", "✅ Projeto atualizado e prazo otimizado recalculado!");
        resposta.put("projetoId", projeto.getId());
        resposta.put("nomeProjeto", projeto.getNome());
        resposta.put("novoPrazoOtimizado", projeto.getPrazoOtimizado());

        return ResponseEntity.ok(resposta);
    }

    // Recalcular prazo otimizado do projeto (manual)
    @PostMapping("/{id}/recalcular")
    public ResponseEntity<Map<String, Object>> recalcularPrazo(@PathVariable Long id) {
        Projeto projeto = projetoService.recalcularPrazoOtimizado(id);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("mensagem", "✅ Prazo otimizado recalculado com sucesso!");
        resposta.put("projetoId", projeto.getId());
        resposta.put("nomeProjeto", projeto.getNome());
        resposta.put("novoPrazoOtimizado", projeto.getPrazoOtimizado());

        return ResponseEntity.ok(resposta);
    }
}