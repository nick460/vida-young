package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.ProductoLandingRequest;
import com.vidayoung.platform.Dto.ProductoLandingResponse;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Service.ProductoLandingService;
import com.vidayoung.platform.Model.Service.ProductoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductoLandingRestController {

    private final ProductoService productoService;
    private final ProductoLandingService productoLandingService;

    @GetMapping("/api/public/productos")
    public ResponseEntity<List<Producto>> listarProductosPublicos() {
        return ResponseEntity.ok(productoService.listarParaShop());
    }

    @GetMapping("/api/public/productos/{productoId}/landing")
    public ResponseEntity<ProductoLandingResponse> buscarLandingPublica(@PathVariable Long productoId) {
        return productoLandingService.buscarPorProductoId(productoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/productos-landings")
    public ResponseEntity<List<ProductoLandingResponse>> listarLandings() {
        return ResponseEntity.ok(productoLandingService.listar());
    }

    @GetMapping("/api/productos/{productoId}/landing")
    public ResponseEntity<ProductoLandingResponse> buscarLanding(@PathVariable Long productoId) {
        return productoLandingService.buscarPorProductoId(productoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/productos/{productoId}/landing")
    public ResponseEntity<ProductoLandingResponse> guardarLanding(
            @PathVariable Long productoId,
            @RequestBody ProductoLandingRequest request
    ) {
        return ResponseEntity.ok(productoLandingService.guardar(productoId, request));
    }
}
