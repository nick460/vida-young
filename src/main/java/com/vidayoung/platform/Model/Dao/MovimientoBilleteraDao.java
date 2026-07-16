package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoBilleteraDao extends JpaRepository<MovimientoBilletera, Long> {

    List<MovimientoBilletera> findByBilleteraPersonaIdOrderByFechaRegistroDesc(Long personaId);

    List<MovimientoBilletera> findByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);

    boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);
}
