package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodoGestionDao extends JpaRepository<PeriodoGestion, Long> {

    Optional<PeriodoGestion> findFirstByEstadoPeriodoOrderByFechaInicioDesc(String estadoPeriodo);

    Optional<PeriodoGestion> findByGestionIdAndMes(Long gestionId, Integer mes);

    List<PeriodoGestion> findByGestionIdOrderByMes(Long gestionId);
}
