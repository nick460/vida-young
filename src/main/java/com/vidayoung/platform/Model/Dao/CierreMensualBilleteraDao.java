package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CierreMensualBilleteraDao extends JpaRepository<CierreMensualBilletera, Long> {

    List<CierreMensualBilletera> findByPersonaIdOrderByPeriodoDesc(Long personaId);

    boolean existsByPersonaIdAndPeriodo(Long personaId, String periodo);

    @Query("""
            select c from CierreMensualBilletera c
            join fetch c.persona p
            where c.periodoGestion.id = :periodoId
              and (
                   coalesce(c.saldoDinero, 0) > 0
                or coalesce(c.saldoPv, 0) > 0
                or coalesce(c.saldoQp, 0) > 0
                or coalesce(c.saldoCr, 0) > 0
                or coalesce(c.saldoProductos, 0) > 0
              )
            order by p.nombres asc, p.apellidos asc
            """)
    List<CierreMensualBilletera> findConSaldosByPeriodoGestionId(@Param("periodoId") Long periodoId);
}
