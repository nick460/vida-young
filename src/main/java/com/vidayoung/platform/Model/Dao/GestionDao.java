package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Gestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GestionDao extends JpaRepository<Gestion, Long> {

    Optional<Gestion> findByAnio(Integer anio);
}
