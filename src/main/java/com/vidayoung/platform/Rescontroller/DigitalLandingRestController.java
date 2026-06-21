package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.DigitalLandingRequest;
import com.vidayoung.platform.Dto.DigitalLandingResponse;
import com.vidayoung.platform.Model.Service.DigitalLandingService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DigitalLandingRestController {

    private final DigitalLandingService digitalLandingService;

    @GetMapping("/api/public/digital-landings/{slug}")
    public ResponseEntity<DigitalLandingResponse> buscarPublica(@PathVariable String slug) {
        return digitalLandingService.buscarPorSlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/digital-landings")
    public ResponseEntity<List<DigitalLandingResponse>> listar() {
        return ResponseEntity.ok(digitalLandingService.listar());
    }

    @GetMapping("/api/digital-landings/{id}")
    public ResponseEntity<DigitalLandingResponse> buscarPorId(@PathVariable Long id) {
        return digitalLandingService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/digital-landings")
    public ResponseEntity<DigitalLandingResponse> guardar(@RequestBody DigitalLandingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(digitalLandingService.guardar(request));
    }

    @PutMapping("/api/digital-landings/{id}")
    public ResponseEntity<DigitalLandingResponse> actualizar(
            @PathVariable Long id,
            @RequestBody DigitalLandingRequest request
    ) {
        return ResponseEntity.ok(digitalLandingService.actualizar(id, request));
    }

    @DeleteMapping("/api/digital-landings/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        digitalLandingService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
