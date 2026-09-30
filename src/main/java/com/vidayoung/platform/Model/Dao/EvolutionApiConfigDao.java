package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.EvolutionApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvolutionApiConfigDao extends JpaRepository<EvolutionApiConfig, Long> {
}
