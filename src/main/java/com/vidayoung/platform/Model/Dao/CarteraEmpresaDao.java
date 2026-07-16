package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.CarteraEmpresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarteraEmpresaDao extends JpaRepository<CarteraEmpresa, Long> {

    Optional<CarteraEmpresa> findByCodigo(String codigo);
}
