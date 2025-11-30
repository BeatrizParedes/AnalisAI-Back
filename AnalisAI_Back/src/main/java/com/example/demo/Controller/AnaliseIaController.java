package com.example.demo.Controller;

import com.example.demo.DTO.AnaliseTarefaRequest;
import com.example.demo.DTO.AnaliseTarefaResponse;
import com.example.demo.DTO.IssueSummary;
import com.example.demo.Service.AnaliseIaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController

@RequestMapping(value = "/api/analise-ia", produces = "application/json; charset=UTF-8")
//@RequestMapping("/api/analise-ia")
public class AnaliseIaController {

    private final AnaliseIaService iaService;

    public AnaliseIaController(AnaliseIaService iaService) {
        this.iaService = iaService;
    }

    @PostMapping("/analisar")
    public ResponseEntity<?> analisarTarefas() {

        ObjectMapper mapper = new ObjectMapper();

        try {
            // Lê o JSON local
            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("issues_summary.json");

            if (inputStream == null) {
                return ResponseEntity.badRequest()
                        .body("Arquivo issues_summary.json não encontrado.");
            }

            IssueSummary[] issuesArray =
                    mapper.readValue(inputStream, IssueSummary[].class);

            List<IssueSummary> issues = List.of(issuesArray);

            if (issues.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Nenhuma issue encontrada no arquivo local.");
            }

            List<AnaliseTarefaResponse> respostas = new ArrayList<>();

            for (IssueSummary issue : issues) {

                AnaliseTarefaRequest req = new AnaliseTarefaRequest(
                        issue.key(),
                        issue.issuetype(),
                        issue.summary(),
                        issue.status(),
                        issue.assignee(),
                        issue.created(),
                        issue.updated(),
                        List.of() // lista vazia de tarefas relacionadas
                );

                AnaliseTarefaResponse resp = iaService.analisarTarefa(req);
                respostas.add(resp);
            }

            return ResponseEntity.ok(respostas);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erro ao carregar JSON local: " + e.getMessage());
        }
    }
    @PostMapping("/analisar-tarefa")
    public ResponseEntity<?> analisarTarefa(@RequestBody AnaliseTarefaRequest req) {
        AnaliseTarefaResponse resp = iaService.analisarTarefa(req);
        return ResponseEntity.ok(resp);
    }

}
