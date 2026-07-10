package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.ProductoCategoria;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoCategoriaDao extends JpaRepository<ProductoCategoria, Long> {

    Optional<ProductoCategoria> findByNombreIgnoreCase(String nombre);

    Optional<ProductoCategoria> findBySiglaIgnoreCase(String sigla);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsBySiglaIgnoreCase(String sigla);
}
