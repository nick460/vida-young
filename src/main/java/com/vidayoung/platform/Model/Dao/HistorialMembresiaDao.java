package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialMembresiaDao extends JpaRepository<HistorialMembresia, Long> {

    List<HistorialMembresia> findByPersonaIdOrderByFechaInicioDesc(Long personaId);

    List<HistorialMembresia> findByEstadoMembresiaAndFechaFinLessThanEqual(String estadoMembresia, LocalDateTime fechaFin);

    boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);

    boolean existsByPersonaIdAndPeriodoIdAndTipo(Long personaId, Long periodoId, String tipo);
}
