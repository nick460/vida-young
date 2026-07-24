package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Compra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraDao extends JpaRepository<Compra, Long> {

    List<Compra> findByPersonaIdOrderByFechaCompraDesc(Long personaId);

    List<Compra> findByEstadoCompraOrderByFechaCompraDesc(String estadoCompra);

    List<Compra> findByPeriodoIdOrderByFechaCompraDesc(Long periodoId);

    @Query("""
            select distinct c from Compra c
            left join fetch c.detalles d
            left join fetch d.producto
            where c.id in :ids
            """)
    List<Compra> findByIdInWithDetalles(@Param("ids") List<Long> ids);

    List<Compra> findAllByOrderByFechaCompraDesc();
}
