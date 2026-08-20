package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficioActivacionCompraDao extends JpaRepository<BeneficioActivacionCompra, Long> {

    List<BeneficioActivacionCompra> findByBeneficiarioIdOrderByFechaRegistroDesc(Long beneficiarioId);

    List<BeneficioActivacionCompra> findByBeneficiarioIdAndPeriodoId(Long beneficiarioId, Long periodoId);

    List<BeneficioActivacionCompra> findByCompraId(Long compraId);

    @Query("""
            select distinct b.beneficiario.id from BeneficioActivacionCompra b
            where b.periodo.id = :periodoId
              and b.estado = :estado
            """)
    List<Long> findBeneficiarioIdsByPeriodoIdAndEstado(@Param("periodoId") Long periodoId, @Param("estado") String estado);
}
