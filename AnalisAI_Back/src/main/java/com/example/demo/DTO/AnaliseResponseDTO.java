package com.example.demo.DTO;

import com.example.demo.model.Analise;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para exibir os resultados de uma análise (individual ou em lista) para o frontend.
 * Mapeia os dados salvos da entidade Analise.
 */
@Data
@NoArgsConstructor
public class AnaliseResponseDTO {

    private Long id;
    private LocalDateTime dataGeracao;
    private String jqlUtilizado;
    private String identificacaoGargalos;
    private String dependenciasIdentificadas;
    private String sugestaoPlanoAcao;

    /**
     * Construtor de conveniência para mapear facilmente da Entidade (Model) para este DTO.
     * @param entity A entidade Analise vinda do banco.
     */
    public AnaliseResponseDTO(Analise entity) {
        this.id = entity.getId();
        this.dataGeracao = entity.getDataGeracao();
        this.jqlUtilizado = entity.getJqlUtilizado();
        this.identificacaoGargalos = entity.getIdentificacaoGargalos();
        this.dependenciasIdentificadas = entity.getDependenciasIdentificadas();
        this.sugestaoPlanoAcao = entity.getSugestaoPlanoAcao();
    }
}