package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.DigitalLanding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalLandingDao extends JpaRepository<DigitalLanding, Long> {

    Optional<DigitalLanding> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
