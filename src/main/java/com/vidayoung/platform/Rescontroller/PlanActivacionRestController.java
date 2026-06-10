package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.PlanActivacionProyeccionResponse;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Service.PlanActivacionService;
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
@RequestMapping("/api/planes-activacion")
@RequiredArgsConstructor
public class PlanActivacionRestController {

    private final PlanActivacionService planActivacionService;

    @GetMapping
    public ResponseEntity<List<PlanActivacion>> listar() {
        return ResponseEntity.ok(planActivacionService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanActivacion> buscarPorId(@PathVariable Long id) {
        return planActivacionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlanActivacion> guardar(@RequestBody PlanActivacion planActivacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planActivacionService.guardar(planActivacion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanActivacion> actualizar(@PathVariable Long id, @RequestBody PlanActivacion planActivacion) {
        return planActivacionService.buscarPorId(id)
                .map(actual -> {
                    planActivacion.setId(id);
                    return ResponseEntity.ok(planActivacionService.guardar(planActivacion));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/niveles")
    public ResponseEntity<PlanActivacionNivel> guardarNivel(@PathVariable Long id, @RequestBody PlanActivacionNivel nivel) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planActivacionService.guardarNivel(id, nivel));
    }

    @DeleteMapping("/niveles/{nivelId}")
    public ResponseEntity<Void> eliminarNivel(@PathVariable Long nivelId) {
        planActivacionService.eliminarNivel(nivelId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (planActivacionService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        planActivacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/persona/{personaId}/proyeccion")
    public ResponseEntity<PlanActivacionProyeccionResponse> proyectarPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(planActivacionService.proyectarPorPersona(personaId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
