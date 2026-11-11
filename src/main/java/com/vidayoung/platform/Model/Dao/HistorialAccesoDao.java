package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.HistorialAcceso;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialAccesoDao extends JpaRepository<HistorialAcceso, Long> {

    Optional<HistorialAcceso> findFirstByPersonaIdAndFechaSalidaIsNullOrderByFechaIngresoDesc(Long personaId);
}
