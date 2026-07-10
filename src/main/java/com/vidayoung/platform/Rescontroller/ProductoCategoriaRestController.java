package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.ProductoCategoriaDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.ProductoCategoria;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
@RequestMapping("/api/producto-categorias")
@RequiredArgsConstructor
public class ProductoCategoriaRestController {

    private final ProductoCategoriaDao productoCategoriaDao;

    @GetMapping
    public ResponseEntity<List<ProductoCategoria>> listar() {
        return ResponseEntity.ok(productoCategoriaDao.findAll().stream()
                .filter(categoria -> Auditoria.ESTADO_ACTIVO.equals(categoria.getEstado()))
                .toList());
    }

    @PostMapping
    public ResponseEntity<ProductoCategoria> guardar(@RequestBody ProductoCategoriaRequest request) {
        ProductoCategoria categoria = ProductoCategoria.builder().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(saveCategoria(categoria, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoCategoria> actualizar(@PathVariable Long id, @RequestBody ProductoCategoriaRequest request) {
        return productoCategoriaDao.findById(id)
                .filter(categoria -> Auditoria.ESTADO_ACTIVO.equals(categoria.getEstado()))
                .map(categoria -> ResponseEntity.ok(saveCategoria(categoria, request)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return productoCategoriaDao.findById(id)
                .filter(categoria -> Auditoria.ESTADO_ACTIVO.equals(categoria.getEstado()))
                .map(categoria -> {
                    categoria.setEstado(Auditoria.ESTADO_ELIMINADO);
                    productoCategoriaDao.save(categoria);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ProductoCategoria saveCategoria(ProductoCategoria categoria, ProductoCategoriaRequest request) {
        String nombre = required(request.getNombre(), "El nombre de la categoria es obligatorio.");
        String sigla = required(request.getSigla(), "La sigla de la categoria es obligatoria.")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");

        if (sigla.length() < 2 || sigla.length() > 12) {
            throw new IllegalArgumentException("La sigla debe tener entre 2 y 12 caracteres alfanumericos.");
        }

        productoCategoriaDao.findByNombreIgnoreCase(nombre)
                .filter(existing -> !existing.getId().equals(categoria.getId()))
                .filter(existing -> Auditoria.ESTADO_ACTIVO.equals(existing.getEstado()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una categoria con ese nombre.");
                });

        productoCategoriaDao.findBySiglaIgnoreCase(sigla)
                .filter(existing -> !existing.getId().equals(categoria.getId()))
                .filter(existing -> Auditoria.ESTADO_ACTIVO.equals(existing.getEstado()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una categoria con esa sigla.");
                });

        categoria.setNombre(nombre);
        categoria.setSigla(sigla);
        categoria.setEstado(Auditoria.ESTADO_ACTIVO);
        return productoCategoriaDao.save(categoria);
    }

    private String required(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @Getter
    @Setter
    public static class ProductoCategoriaRequest {
        private String nombre;
        private String sigla;
    }
}
