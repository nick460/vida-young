package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Referido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferidoDao extends JpaRepository<Referido, Long> {

    Optional<Referido> findByPersonaId(Long personaId);

    List<Referido> findByPatrocinadorId(Long patrocinadorId);

    List<Referido> findByMembresiaActivaTrueAndFechaFinMembresiaLessThanEqual(LocalDateTime fechaFin);

    boolean existsByPersonaId(Long personaId);
}
