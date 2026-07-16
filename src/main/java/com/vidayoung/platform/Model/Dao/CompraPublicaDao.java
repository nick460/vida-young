package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.CompraPublica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraPublicaDao extends JpaRepository<CompraPublica, Long> {

    List<CompraPublica> findAllByOrderByFechaCompraDesc();

    List<CompraPublica> findByDistribuidorIdOrderByFechaCompraDesc(Long distribuidorId);

    Optional<CompraPublica> findFirstByClienteDocumentoOrderByFechaCompraDesc(String clienteDocumento);
}
