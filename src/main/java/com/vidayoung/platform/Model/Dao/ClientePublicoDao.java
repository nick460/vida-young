package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.ClientePublico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientePublicoDao extends JpaRepository<ClientePublico, Long> {

    List<ClientePublico> findAllByOrderByFechaRegistroDesc();

    List<ClientePublico> findByDistribuidorIdOrderByFechaRegistroDesc(Long distribuidorId);

    List<ClientePublico> findByTipoClienteIdOrderByFechaRegistroDesc(Long tipoClienteId);

    Optional<ClientePublico> findByDistribuidorIdAndDocumentoIgnoreCase(Long distribuidorId, String documento);
}

