package com.vidayoung.platform.Model.Dao;

import com.vidayoung.platform.Model.Entity.MenuSistema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuSistemaDao extends JpaRepository<MenuSistema, Long> {

    List<MenuSistema> findAllByOrderByOrdenAscIdAsc();

    Optional<MenuSistema> findByMenuId(String menuId);

    boolean existsByMenuId(String menuId);
}
