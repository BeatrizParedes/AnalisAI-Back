package com.example.demo.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa um registro de análise de IA salvo no banco de dados.
 * Esta tabela armazena o "histórico de análises e recomendações".
 */
@Entity
@Table(name = "analise_historico")
@Data
@NoArgsConstructor
public class Analise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataGeracao;

    /**
     * O JQL usado para buscar as issues que foram analisadas.
     * Pode ser o JQL padrão do application.properties ou um customizado.
     */
    @Column(columnDefinition = "TEXT")
    private String jqlUtilizado;

    /**
     * O resultado da IA sobre gargalos.
     */
    @Lob // Large Object, para textos longos de resposta da IA
    @Column(columnDefinition = "TEXT")
    private String identificacaoGargalos;

    /**
     * O resultado da IA sobre dependências.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String dependenciasIdentificadas;

    /**
     * O plano de ação ou sugestão de otimização gerado pela IA.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String sugestaoPlanoAcao;

    // Getters, Setters, etc., são gerados pelo Lombok (@Data)
}