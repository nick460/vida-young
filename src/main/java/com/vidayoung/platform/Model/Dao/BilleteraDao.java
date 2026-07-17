package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.Billetera;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BilleteraDao extends JpaRepository<Billetera, Long> {

    Optional<Billetera> findByPersonaId(Long personaId);

    @Query("""
            select b from Billetera b
            join fetch b.persona p
            where coalesce(b.saldoDinero, 0) > 0
               or coalesce(b.saldoPv, 0) > 0
               or coalesce(b.saldoQp, 0) > 0
               or coalesce(b.saldoCr, 0) > 0
               or coalesce(b.saldoProductos, 0) > 0
            order by p.nombres asc, p.apellidos asc
            """)
    List<Billetera> findAllConSaldos();
}
