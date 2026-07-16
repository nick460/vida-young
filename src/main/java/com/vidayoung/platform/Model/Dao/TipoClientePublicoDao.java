package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.TipoClientePublico;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoClientePublicoDao extends JpaRepository<TipoClientePublico, Long> {

    Optional<TipoClientePublico> findByCodigoIgnoreCase(String codigo);
}

