package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Compra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraDao extends JpaRepository<Compra, Long> {

    List<Compra> findByPersonaIdOrderByFechaCompraDesc(Long personaId);

    List<Compra> findByEstadoCompraOrderByFechaCompraDesc(String estadoCompra);

    List<Compra> findByPeriodoIdOrderByFechaCompraDesc(Long periodoId);

    List<Compra> findAllByOrderByFechaCompraDesc();
}
