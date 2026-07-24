package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RetiroBilleteraDao extends JpaRepository<RetiroBilletera, Long> {

    List<RetiroBilletera> findByPersonaIdOrderByFechaRetiroDesc(Long personaId);

    @Query("""
            select r from RetiroBilletera r
            join fetch r.persona p
            left join fetch r.periodo periodo
            left join fetch periodo.gestion
            where r.periodo.id = :periodoId
            order by r.fechaRetiro desc
            """)
    List<RetiroBilletera> findByPeriodoIdWithPersonaOrderByFechaRetiroDesc(@Param("periodoId") Long periodoId);
}
