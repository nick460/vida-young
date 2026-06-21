package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.ProductoLanding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoLandingDao extends JpaRepository<ProductoLanding, Long> {

    Optional<ProductoLanding> findByProductoId(Long productoId);
}
