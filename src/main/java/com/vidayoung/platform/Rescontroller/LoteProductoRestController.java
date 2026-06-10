package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.LoteProducto;
import com.vidayoung.platform.Model.Service.LoteProductoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotes-productos")
@RequiredArgsConstructor
public class LoteProductoRestController {

    private final LoteProductoService loteProductoService;

    @GetMapping
    public ResponseEntity<List<LoteProducto>> listar(@RequestParam(required = false) Long productoId) {
        if (productoId != null) {
            return ResponseEntity.ok(loteProductoService.listarPorProducto(productoId));
        }

        return ResponseEntity.ok(loteProductoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteProducto> buscarPorId(@PathVariable Long id) {
        return loteProductoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LoteProducto> guardar(@RequestBody LoteProducto loteProducto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteProductoService.guardar(loteProducto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteProducto> actualizar(@PathVariable Long id, @RequestBody LoteProducto loteProducto) {
        return loteProductoService.buscarPorId(id)
                .map(loteActual -> {
                    loteProducto.setId(id);
                    return ResponseEntity.ok(loteProductoService.guardar(loteProducto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (loteProductoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        loteProductoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
