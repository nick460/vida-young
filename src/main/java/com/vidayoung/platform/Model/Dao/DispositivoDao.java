package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Dispositivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispositivoDao extends JpaRepository<Dispositivo, Long> {

    Optional<Dispositivo> findByToken(String token);

    List<Dispositivo> findByPersonaId(Long personaId);

    List<Dispositivo> findByPersonaIdAndEstado(Long personaId, String estado);

    Optional<Dispositivo> findByTokenAndEstado(String token, String estado);

    long countByPersonaIdAndActivoTrue(Long personaId);

    List<Dispositivo> findByPersonaIdAndActivoTrue(Long personaId);

    List<Dispositivo> findByActivoTrue();

    void deleteByToken(String token);

    boolean existsByToken(String token);
}
