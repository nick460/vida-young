package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraDetalleDao extends JpaRepository<CompraDetalle, Long> {
}
