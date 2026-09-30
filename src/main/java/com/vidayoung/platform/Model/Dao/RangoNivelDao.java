package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.RangoNivel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RangoNivelDao extends JpaRepository<RangoNivel, Long> {

    List<RangoNivel> findByRangoId(Long rangoId);

    Optional<RangoNivel> findByRangoIdAndNumeroNivelExtra(Long rangoId, Integer numeroNivelExtra);
}
