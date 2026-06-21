package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.ProductoLandingRequest;
import com.vidayoung.platform.Dto.ProductoLandingResponse;
import java.util.List;
import java.util.Optional;

public interface ProductoLandingService {

    List<ProductoLandingResponse> listar();

    Optional<ProductoLandingResponse> buscarPorProductoId(Long productoId);

    ProductoLandingResponse guardar(Long productoId, ProductoLandingRequest request);
}
