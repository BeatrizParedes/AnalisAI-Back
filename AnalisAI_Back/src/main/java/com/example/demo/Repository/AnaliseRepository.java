package com.example.demo.Repository;

import com.example.demo.Model.Analise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnaliseRepository extends JpaRepository<Analise, Long> {

    /**
     * Busca todas as análises ordenadas pela data de geração, da mais recente para a mais antiga.
     * @return Lista de análises ordenadas.
     */
    List<Analise> findAllByOrderByDataGeracaoDesc();
}
