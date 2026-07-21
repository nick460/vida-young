package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.PreinscripcionReferido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreinscripcionReferidoDao extends JpaRepository<PreinscripcionReferido, Long> {

    List<PreinscripcionReferido> findByEstadoPreinscripcionOrderByFechaRegistroDesc(String estadoPreinscripcion);

    Optional<PreinscripcionReferido> findByDocumentoAndEstadoPreinscripcion(String documento, String estadoPreinscripcion);

    Optional<PreinscripcionReferido> findByUsernameSolicitadoAndEstadoPreinscripcion(String usernameSolicitado, String estadoPreinscripcion);
}
