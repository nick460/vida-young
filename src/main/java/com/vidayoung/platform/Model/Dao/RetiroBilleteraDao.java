package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetiroBilleteraDao extends JpaRepository<RetiroBilletera, Long> {

    List<RetiroBilletera> findByPersonaIdOrderByFechaRetiroDesc(Long personaId);
}
