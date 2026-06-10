package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanActivacionNivelDao extends JpaRepository<PlanActivacionNivel, Long> {

    List<PlanActivacionNivel> findByPlanActivacionId(Long planActivacionId);

    Optional<PlanActivacionNivel> findByPlanActivacionIdAndNumeroNivel(Long planActivacionId, Integer numeroNivel);
}
