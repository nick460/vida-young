package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Producto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoDao extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    Optional<Producto> findTopBySkuStartingWithIgnoreCaseOrderBySkuDesc(String prefix);
}
