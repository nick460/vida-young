package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Rango;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RangoDao extends JpaRepository<Rango, Long> {
}
