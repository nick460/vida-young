package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Service.ProductoService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoRestController {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Path PRODUCT_UPLOAD_DIR = Paths.get("uploads", "productos");

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Producto> buscarPorSku(@PathVariable String sku) {
        return productoService.buscarPorSku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Producto> guardar(@RequestBody Producto producto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.guardar(producto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @RequestBody Producto producto) {
        return productoService.buscarPorId(id)
                .map(productoActual -> {
                    producto.setId(id);
                    return ResponseEntity.ok(productoService.guardar(producto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/imagen")
    public ResponseEntity<Producto> subirImagen(
            @PathVariable Long id,
            @RequestPart("imagen") MultipartFile imagen
    ) throws IOException {
        if (imagen.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = imagen.getContentType();
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        return productoService.buscarPorId(id)
                .map(producto -> {
                    try {
                        Files.createDirectories(PRODUCT_UPLOAD_DIR);
                        String extension = StringUtils.getFilenameExtension(imagen.getOriginalFilename());
                        String safeExtension = extension == null ? "jpg" : extension.toLowerCase(Locale.ROOT);
                        String fileName = "producto-" + id + "-" + UUID.randomUUID() + "." + safeExtension;
                        Path destino = PRODUCT_UPLOAD_DIR.resolve(fileName).normalize();
                        imagen.transferTo(destino);

                        producto.setImagenUrl("/uploads/productos/" + fileName);
                        return ResponseEntity.ok(productoService.guardar(producto));
                    } catch (IOException exception) {
                        throw new IllegalStateException("No se pudo guardar la imagen del producto", exception);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (productoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
