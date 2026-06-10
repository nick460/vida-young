package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.LoteProducto;
import java.util.List;
import java.util.Optional;

public interface LoteProductoService {

    List<LoteProducto> listar();

    List<LoteProducto> listarPorProducto(Long productoId);

    Optional<LoteProducto> buscarPorId(Long id);

    LoteProducto guardar(LoteProducto loteProducto);

    void eliminar(Long id);
}
