package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Persona;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaDao extends JpaRepository<Persona, Long> {

    Optional<Persona> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}
