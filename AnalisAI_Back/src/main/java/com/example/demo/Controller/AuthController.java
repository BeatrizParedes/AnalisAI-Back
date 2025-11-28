package com.example.demo.controller;

import com.example.demo.service.JiraAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JiraAuthService jiraAuthService;

    public AuthController(JiraAuthService jiraAuthService) {
        this.jiraAuthService = jiraAuthService;
    }

    /**
     * Autentica com email + API token (Basic Auth)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam("apiToken") String apiToken) {
        log.info("🔐 Tentando autenticar no Jira com email {}", email);

        try {
            boolean ok = jiraAuthService.testCredentials(email, apiToken);

            if (ok) {
                log.info("✅ Credenciais Jira válidas!");
                return ResponseEntity.ok("Autenticado com sucesso!");
            } else {
                log.warn("❌ Credenciais inválidas!");
                return ResponseEntity.status(401).body("Credenciais inválidas para o Jira");
            }

        } catch (Exception e) {
            log.error("❌ Erro ao validar credenciais Jira: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erro ao validar credenciais: " + e.getMessage());
        }
    }
}
