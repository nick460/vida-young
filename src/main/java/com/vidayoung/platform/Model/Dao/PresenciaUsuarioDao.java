package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PresenciaUsuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PresenciaUsuarioDao extends JpaRepository<PresenciaUsuario, Long> {

    Optional<PresenciaUsuario> findByPersonaId(Long personaId);

    List<PresenciaUsuario> findByUltimoLatidoAfterOrderByUltimoLatidoDesc(LocalDateTime desde);

    void deleteByUltimoLatidoBefore(LocalDateTime antes);
}
