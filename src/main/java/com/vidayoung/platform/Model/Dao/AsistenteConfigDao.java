package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.AsistenteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsistenteConfigDao extends JpaRepository<AsistenteConfig, Long> {
}
