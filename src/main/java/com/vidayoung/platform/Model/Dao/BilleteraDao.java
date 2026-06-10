package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Billetera;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BilleteraDao extends JpaRepository<Billetera, Long> {

    Optional<Billetera> findByPersonaId(Long personaId);
}
