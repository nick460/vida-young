package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PlanProducto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanProductoDao extends JpaRepository<PlanProducto, Long> {

    Optional<PlanProducto> findByPlanIdAndProductoId(Long planId, Long productoId);
}
