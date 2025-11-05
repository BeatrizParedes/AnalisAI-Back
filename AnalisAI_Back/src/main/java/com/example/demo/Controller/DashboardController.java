package com.example.demo.Controller;

import com.example.demo.DTO.DashboardStatsDTO;
import com.example.demo.Service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Endpoint principal para obter as estatísticas do dashboard.
     * Requer o token de autenticação OAuth 2.0.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(
            @RequestHeader("Authorization") String authorization
    ) {
        // Extrai o token e passa para o serviço
        String accessToken = extractToken(authorization);
        DashboardStatsDTO stats = dashboardService.getProjectStats(accessToken);
        return ResponseEntity.ok(stats);
    }

    /**
     * Extrai o token do header Authorization.
     * Exemplo: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Cabeçalho Authorization inválido ou ausente. Deve ser 'Bearer <token>'");
        }
        return authorizationHeader.substring(7);
    }
}

