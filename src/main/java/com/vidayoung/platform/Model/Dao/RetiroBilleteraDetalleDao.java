package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.RetiroBilleteraDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetiroBilleteraDetalleDao extends JpaRepository<RetiroBilleteraDetalle, Long> {

    List<RetiroBilleteraDetalle> findByRetiroId(Long retiroId);
}
