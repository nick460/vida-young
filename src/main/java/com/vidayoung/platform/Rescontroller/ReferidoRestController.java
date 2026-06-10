package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.ReferidoService;
import java.util.List;
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
@RequestMapping("/api/referidos")
@RequiredArgsConstructor
public class ReferidoRestController {

    private final ReferidoService referidoService;

    @GetMapping
    public ResponseEntity<List<Referido>> listar() {
        return ResponseEntity.ok(referidoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Referido> buscarPorId(@PathVariable Long id) {
        return referidoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<Referido> buscarPorPersona(@PathVariable Long personaId) {
        return referidoService.buscarPorPersonaId(personaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patrocinador/{patrocinadorId}")
    public ResponseEntity<List<Referido>> listarPorPatrocinador(@PathVariable Long patrocinadorId) {
        return ResponseEntity.ok(referidoService.listarPorPatrocinador(patrocinadorId));
    }

    @PostMapping
    public ResponseEntity<Referido> guardar(@RequestBody ReferidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referidoService.guardar(request.getPersonaId(), request.getPatrocinadorId(), request.getPlanId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Referido> actualizar(@PathVariable Long id, @RequestBody ReferidoRequest request) {
        return ResponseEntity.ok(
                referidoService.actualizar(id, request.getPersonaId(), request.getPatrocinadorId(), request.getPlanId())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (referidoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        referidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @Getter
    @Setter
    public static class ReferidoRequest {
        private Long personaId;
        private Long patrocinadorId;
        private Long planId;
    }
}
