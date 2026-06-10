package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PlanActivacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanActivacionDao extends JpaRepository<PlanActivacion, Long> {

    Optional<PlanActivacion> findByNombre(String nombre);

    List<PlanActivacion> findByPvMinimoMensualLessThanEqualOrderByPvMinimoMensualDesc(BigDecimal pvMensual);
}
