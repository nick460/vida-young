package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.LoteProducto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteProductoDao extends JpaRepository<LoteProducto, Long> {

    List<LoteProducto> findByProductoId(Long productoId);
}
