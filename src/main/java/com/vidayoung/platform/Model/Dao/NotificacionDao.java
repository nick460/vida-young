package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Notificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionDao extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioIdAndEstadoOrderByFechaEnviadoDesc(Long destinatarioId, String estado);

    List<Notificacion> findByDestinatarioIsNullAndEstadoOrderByFechaEnviadoDesc(String estado);

    long countByDestinatarioIdAndEstadoAndLeidaFalse(Long destinatarioId, String estado);

    long countByDestinatarioIsNullAndEstadoAndLeidaFalse(String estado);

    @Query("""
            select n from Notificacion n
            where n.estado = :estado
              and (n.destinatario is null or n.destinatario.id = :destinatarioId)
            order by n.fechaEnviado desc
            """)
    List<Notificacion> findMias(@Param("destinatarioId") Long destinatarioId, @Param("estado") String estado);

    @Query("""
            select count(n) from Notificacion n
            where n.estado = :estado
              and n.leida = false
              and (n.destinatario is null or n.destinatario.id = :destinatarioId)
            """)
    long countMiasNoLeidas(@Param("destinatarioId") Long destinatarioId, @Param("estado") String estado);
}
