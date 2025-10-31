package com.example.demo.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    // duração estimada em dias
    private int duracaoDias;

    // marca se é um gargalo (afeta cálculo)
    private boolean gargalo;

    private boolean concluida;

    @ManyToOne
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;
}