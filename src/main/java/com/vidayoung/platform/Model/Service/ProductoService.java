package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoService {

    List<Producto> listar();

    Optional<Producto> buscarPorId(Long id);

    Optional<Producto> buscarPorSku(String sku);

    Producto guardar(Producto producto);

    void eliminar(Long id);
}
