package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PresenciaUsuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PresenciaUsuarioDao extends JpaRepository<PresenciaUsuario, Long> {

    Optional<PresenciaUsuario> findByPersonaId(Long personaId);

    @Modifying
    @Query(value = """
            INSERT INTO presencias_usuarios (
                estado, fecha_modificacion, fecha_registro, ip, persona_id, ruta_actual,
                ultimo_latido, user_agent, username, usuario_modificacion, usuario_registro
            ) VALUES (
                'ACTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :ip, :personaId, :rutaActual,
                CURRENT_TIMESTAMP, :userAgent, :username, :username, :username
            )
            ON CONFLICT (persona_id) DO UPDATE SET
                estado = 'ACTIVO',
                fecha_modificacion = CURRENT_TIMESTAMP,
                ip = EXCLUDED.ip,
                ruta_actual = EXCLUDED.ruta_actual,
                ultimo_latido = CURRENT_TIMESTAMP,
                user_agent = EXCLUDED.user_agent,
                username = EXCLUDED.username,
                usuario_modificacion = EXCLUDED.username
            """, nativeQuery = true)
    void registrarLatido(
            @Param("personaId") Long personaId,
            @Param("username") String username,
            @Param("ip") String ip,
            @Param("userAgent") String userAgent,
            @Param("rutaActual") String rutaActual
    );

    List<PresenciaUsuario> findByUltimoLatidoAfterOrderByUltimoLatidoDesc(LocalDateTime desde);

    void deleteByUltimoLatidoBefore(LocalDateTime antes);
}
