package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Recompensa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecompensaDao extends JpaRepository<Recompensa, Long> {

    List<Recompensa> findByBeneficiarioId(Long beneficiarioId);

    List<Recompensa> findByReferidoId(Long referidoId);
}
