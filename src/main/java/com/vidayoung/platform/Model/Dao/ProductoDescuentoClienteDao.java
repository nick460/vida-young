package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.ProductoDescuentoCliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoDescuentoClienteDao extends JpaRepository<ProductoDescuentoCliente, Long> {

    List<ProductoDescuentoCliente> findByTipoClienteId(Long tipoClienteId);

    Optional<ProductoDescuentoCliente> findByProductoIdAndTipoClienteId(Long productoId, Long tipoClienteId);
}

