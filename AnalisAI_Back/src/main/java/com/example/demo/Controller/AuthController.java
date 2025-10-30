package com.example.demo.controller;

import com.example.demo.service.AuthService;
import com.example.demo.DTO.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

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

    /** ✅ Inicia o fluxo OAuth redirecionando para a tela de login da Atlassian */
    @GetMapping("/login")
    public RedirectView login() {
        String authorizationUrl = authService.buildAuthorizationUrl(clientId, redirectUri, scope);
        log.info("Redirecionando usuário para Atlassian: {}", authorizationUrl);
        return new RedirectView(authorizationUrl);
    }

    /** ✅ Callback que recebe o "code" e troca por access_token */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription
    ) {
        if (error != null) {
            log.error("Erro recebido da Atlassian: {} - {}", error, errorDescription);
            return ResponseEntity.badRequest().body("Erro Atlassian: " + error + " - " + errorDescription);
        }

        if (code == null) {
            log.error("Callback recebido sem parâmetro 'code'.");
            return ResponseEntity.badRequest().body("Erro: parâmetro 'code' ausente na callback.");
        }

        try {
            log.info("Recebido authorization code: {}", code);
            TokenResponse tokenResponse = authService.exchangeCodeForToken(code);
            log.info("Token obtido com sucesso: {}", tokenResponse);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("Erro ao obter token da Atlassian: ", e);
            return ResponseEntity.status(500).body("Erro ao obter token: " + e.getMessage());
        }
    }

    /** ✅ Endpoint para renovar o token com refresh_token */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        try {
            TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("Erro ao renovar token: ", e);
            return ResponseEntity.status(500).body("Erro ao renovar token: " + e.getMessage());
        }
    }
}

