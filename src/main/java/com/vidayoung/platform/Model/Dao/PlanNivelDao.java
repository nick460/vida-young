package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PlanNivel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanNivelDao extends JpaRepository<PlanNivel, Long> {

    Optional<PlanNivel> findByPlanIdAndNumeroNivel(Long planId, Integer numeroNivel);

    List<PlanNivel> findByPlanId(Long planId);
}
