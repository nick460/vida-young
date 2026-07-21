package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanNivel;
import com.vidayoung.platform.Model.Entity.PlanProducto;
import com.vidayoung.platform.Model.Service.PlanService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
@RequestMapping("/api/planes")
@RequiredArgsConstructor
public class PlanRestController {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Path PLAN_UPLOAD_DIR = Paths.get("uploads", "planes");

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<Plan>> listar() {
        return ResponseEntity.ok(planService.listar());
    }

    @GetMapping("/public")
    public ResponseEntity<List<Plan>> listarPublicos() {
        return ResponseEntity.ok(planService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plan> buscarPorId(@PathVariable Long id) {
        return planService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<Plan> buscarPorNombre(@PathVariable String nombre) {
        return planService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Plan> guardar(@RequestBody Plan plan) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.guardar(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Plan> actualizar(@PathVariable Long id, @RequestBody Plan plan) {
        return planService.buscarPorId(id)
                .map(planActual -> {
                    plan.setId(id);
                    return ResponseEntity.ok(planService.guardar(plan));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/niveles")
    public ResponseEntity<PlanNivel> guardarNivel(@PathVariable Long id, @RequestBody PlanNivel nivel) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.guardarNivel(id, nivel));
    }

    @PostMapping("/{id}/productos")
    public ResponseEntity<PlanProducto> guardarProducto(
            @PathVariable Long id,
            @RequestBody PlanProductoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.guardarProducto(id, request.getProductoId(), request.getCantidad()));
    }

    @PostMapping("/{id}/imagen")
    public ResponseEntity<Plan> subirImagen(
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

        return planService.buscarPorId(id)
                .map(plan -> {
                    try {
                        Files.createDirectories(PLAN_UPLOAD_DIR);
                        String extension = StringUtils.getFilenameExtension(imagen.getOriginalFilename());
                        String safeExtension = extension == null ? "jpg" : extension.toLowerCase(Locale.ROOT);
                        String fileName = "plan-" + id + "-" + UUID.randomUUID() + "." + safeExtension;
                        Path destino = PLAN_UPLOAD_DIR.resolve(fileName).normalize();
                        imagen.transferTo(destino);

                        plan.setImagenUrl("/uploads/planes/" + fileName);
                        return ResponseEntity.ok(planService.guardar(plan));
                    } catch (IOException exception) {
                        throw new IllegalStateException("No se pudo guardar la imagen del plan", exception);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/niveles/{nivelId}")
    public ResponseEntity<Void> eliminarNivel(@PathVariable Long nivelId) {
        planService.eliminarNivel(nivelId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/productos/{planProductoId}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long planProductoId) {
        planService.eliminarProducto(planProductoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (planService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        planService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @Getter
    @Setter
    public static class PlanProductoRequest {
        private Long productoId;
        private Integer cantidad;
    }
}
