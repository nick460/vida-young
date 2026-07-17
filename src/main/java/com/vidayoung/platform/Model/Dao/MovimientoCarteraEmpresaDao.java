package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.MovimientoCarteraEmpresa;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoCarteraEmpresaDao extends JpaRepository<MovimientoCarteraEmpresa, Long> {

    List<MovimientoCarteraEmpresa> findByCarteraIdOrderByFechaRegistroDesc(Long carteraId);

    List<MovimientoCarteraEmpresa> findByCarteraIdAndPeriodoIdOrderByFechaRegistroDesc(Long carteraId, Long periodoId);

    @Query("""
            select coalesce(sum(m.monto), 0)
            from MovimientoCarteraEmpresa m
            where m.cartera.id = :carteraId
              and m.estado = 'ACTIVO'
              and m.periodo.fechaInicio < (
                    select p.fechaInicio
                    from PeriodoGestion p
                    where p.id = :periodoId
              )
            """)
    BigDecimal sumMontoBeforePeriodo(@Param("carteraId") Long carteraId, @Param("periodoId") Long periodoId);

    @Query("""
            select coalesce(sum(m.monto), 0)
            from MovimientoCarteraEmpresa m
            where m.cartera.id = :carteraId
              and m.estado = 'ACTIVO'
              and m.periodo.id = :periodoId
            """)
    BigDecimal sumMontoByPeriodo(@Param("carteraId") Long carteraId, @Param("periodoId") Long periodoId);

    boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);
}
