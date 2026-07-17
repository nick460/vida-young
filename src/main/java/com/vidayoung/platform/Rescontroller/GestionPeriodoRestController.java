package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Gestion;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gestiones")
@RequiredArgsConstructor
public class GestionPeriodoRestController {

    private final GestionPeriodoService gestionPeriodoService;

    @GetMapping
    public ResponseEntity<List<Gestion>> listarGestiones() {
        return ResponseEntity.ok(gestionPeriodoService.listarGestiones());
    }

    @PostMapping
    public ResponseEntity<Gestion> crearGestion(@RequestBody GestionRequest request) {
        return ResponseEntity.ok(gestionPeriodoService.crearGestion(request.getAnio(), request.getNombre()));
    }

    @PutMapping("/{gestionId}")
    public ResponseEntity<Gestion> actualizarGestion(
            @PathVariable Long gestionId,
            @RequestBody GestionRequest request
    ) {
        return ResponseEntity.ok(gestionPeriodoService.actualizarGestion(gestionId, request.getAnio(), request.getNombre()));
    }

    @GetMapping("/{gestionId}/periodos")
    public ResponseEntity<List<PeriodoGestion>> listarPeriodos(@PathVariable Long gestionId) {
        return ResponseEntity.ok(gestionPeriodoService.listarPeriodos(gestionId));
    }

    @PostMapping("/{gestionId}/periodos")
    public ResponseEntity<PeriodoGestion> crearPeriodo(
            @PathVariable Long gestionId,
            @RequestBody PeriodoRequest request
    ) {
        return ResponseEntity.ok(gestionPeriodoService.crearPeriodo(gestionId, request.getMes(), request.getNombre()));
    }

    @PutMapping("/periodos/{periodoId}")
    public ResponseEntity<PeriodoGestion> actualizarPeriodo(
            @PathVariable Long periodoId,
            @RequestBody PeriodoRequest request
    ) {
        return ResponseEntity.ok(gestionPeriodoService.actualizarPeriodo(periodoId, request.getNombre()));
    }

    @GetMapping("/periodos/activo")
    public ResponseEntity<PeriodoGestion> obtenerPeriodoActivo() {
        return ResponseEntity.ok(gestionPeriodoService.obtenerPeriodoActivo());
    }

    @PutMapping("/periodos/{periodoId}/activar")
    public ResponseEntity<PeriodoGestion> activarPeriodo(@PathVariable Long periodoId) {
        return ResponseEntity.ok(gestionPeriodoService.activarPeriodo(periodoId));
    }

    @PutMapping("/periodos/activo/cerrar")
    public ResponseEntity<PeriodoGestion> cerrarPeriodoActivo() {
        return ResponseEntity.ok(gestionPeriodoService.cerrarPeriodoActivo());
    }

    @Getter
    @Setter
    public static class GestionRequest {
        private Integer anio;
        private String nombre;
    }

    @Getter
    @Setter
    public static class PeriodoRequest {
        private Integer mes;
        private String nombre;
    }
}
