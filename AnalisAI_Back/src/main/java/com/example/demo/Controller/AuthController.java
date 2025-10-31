package com.example.demo.controller;

import com.example.demo.DTO.TokenResponse;
import com.example.demo.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller responsável pelo fluxo de autenticação OAuth 2.0 da Atlassian (Jira Cloud)
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Value("${atlassian.client-id}")
    private String clientId;

    @Value("${atlassian.redirect-uri}")
    private String redirectUri;

    @Value("${atlassian.scope}")
    private String scope;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * ✅ Inicia o fluxo OAuth redirecionando o usuário para o login Atlassian
     */
    @GetMapping("/login")
    public RedirectView login() {
        String authorizationUrl = authService.buildAuthorizationUrl(clientId, redirectUri, scope);
        log.info("🔸 Redirecionando usuário para autorização Atlassian: {}", authorizationUrl);
        return new RedirectView(authorizationUrl);
    }

    /**
     * ✅ Callback que recebe o "code" de autorização e troca por um access_token
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) {
        if (error != null) {
            log.error("❌ Erro recebido da Atlassian: {} - {}", error, errorDescription);
            return ResponseEntity.badRequest().body("Erro Atlassian: " + error + " - " + errorDescription);
        }

        if (code == null) {
            log.error("❌ Callback recebido sem parâmetro 'code'.");
            return ResponseEntity.badRequest().body("Erro: parâmetro 'code' ausente na callback.");
        }

        try {
            log.info("✅ Authorization code recebido: {}", code);

            TokenResponse tokenResponse = authService.exchangeCodeForToken(code);
            log.info("✅ Token obtido com sucesso. Expira em {} segundos.", tokenResponse.getExpiresIn());

            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("❌ Erro ao trocar authorization code por token Atlassian: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erro ao obter token: " + e.getMessage());
        }
    }

    /**
     * ✅ Endpoint para renovar o access_token usando o refresh_token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        try {
            log.info("🔁 Solicitando refresh do token...");
            TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);
            log.info("✅ Novo token gerado com sucesso.");
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("❌ Erro ao renovar token Atlassian: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erro ao renovar token: " + e.getMessage());
        }
    }
}