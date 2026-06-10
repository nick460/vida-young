package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficioActivacionCompraDao extends JpaRepository<BeneficioActivacionCompra, Long> {

    List<BeneficioActivacionCompra> findByBeneficiarioIdOrderByFechaRegistroDesc(Long beneficiarioId);

    List<BeneficioActivacionCompra> findByCompraId(Long compraId);
}
