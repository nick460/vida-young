package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.MovimientoCarteraEmpresa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoCarteraEmpresaDao extends JpaRepository<MovimientoCarteraEmpresa, Long> {

    List<MovimientoCarteraEmpresa> findByCarteraIdOrderByFechaRegistroDesc(Long carteraId);

    boolean existsByReferenciaTipoAndReferenciaIdAndTipo(String referenciaTipo, Long referenciaId, String tipo);
}
