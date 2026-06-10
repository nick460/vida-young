package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Plan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanDao extends JpaRepository<Plan, Long> {

    Optional<Plan> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
