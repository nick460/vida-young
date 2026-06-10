package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CierreMensualBilleteraDao extends JpaRepository<CierreMensualBilletera, Long> {

    List<CierreMensualBilletera> findByPersonaIdOrderByPeriodoDesc(Long personaId);

    boolean existsByPersonaIdAndPeriodo(Long personaId, String periodo);
}
