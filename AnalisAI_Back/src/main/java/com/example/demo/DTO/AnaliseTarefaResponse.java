package com.example.demo.DTO;

import java.util.List;

public record AnaliseTarefaResponse(
    String idTarefa,
    
    List<String> riscosIdentificados,
    String resumoRiscos,
    
    List<String> dependenciasObrigatorias,
    List<String> dependenciasSugeridas,
    
    List<String> sugestoesOtimizacao
) {}