package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Service.RangoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rangos")
@RequiredArgsConstructor
public class RangoRestController {

    private final RangoService rangoService;

    @GetMapping
    public ResponseEntity<List<Rango>> listar() {
        return ResponseEntity.ok(rangoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rango> buscarPorId(@PathVariable Long id) {
        return rangoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Rango> guardar(@RequestBody Rango rango) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rangoService.guardar(rango));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rango> actualizar(@PathVariable Long id, @RequestBody Rango rango) {
        return rangoService.buscarPorId(id)
                .map(actual -> {
                    rango.setId(id);
                    return ResponseEntity.ok(rangoService.guardar(rango));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (rangoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        rangoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
