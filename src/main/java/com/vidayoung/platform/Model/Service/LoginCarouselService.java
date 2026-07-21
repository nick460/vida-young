package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.LoginCarouselItem;
import java.util.List;
import java.util.Optional;

public interface LoginCarouselService {

    List<LoginCarouselItem> listar();

    List<LoginCarouselItem> listarActivos();

    Optional<LoginCarouselItem> buscarPorId(Long id);

    LoginCarouselItem guardar(LoginCarouselItem item);

    void eliminar(Long id);
}
