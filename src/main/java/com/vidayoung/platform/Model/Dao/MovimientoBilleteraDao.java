package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoBilleteraDao extends JpaRepository<MovimientoBilletera, Long> {

    List<MovimientoBilletera> findByBilleteraPersonaIdOrderByFechaRegistroDesc(Long personaId);

    List<MovimientoBilletera> findByBilleteraPersonaIdAndPeriodoIdOrderByFechaRegistroDesc(Long personaId, Long periodoId);

    @Query("""
            select m from MovimientoBilletera m
            join fetch m.billetera b
            join fetch b.persona p
            where m.periodo.id = :periodoId
            order by p.nombres asc, p.apellidos asc, m.fechaRegistro asc
            """)
    List<MovimientoBilletera> findByPeriodoIdWithPersona(@Param("periodoId") Long periodoId);

    List<MovimientoBilletera> findByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);

    boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);
}
