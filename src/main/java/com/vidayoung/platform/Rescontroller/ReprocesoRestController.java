package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Service.ReprocesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reproceso")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class ReprocesoRestController {

    private final ReprocesoService reprocesoService;

    @GetMapping("/dry-run")
    public ResponseEntity<ReprocesoService.ReprocesoResumen> simular() {
        return ResponseEntity.ok(reprocesoService.simular());
    }

    @PostMapping
    public ResponseEntity<ReprocesoService.ReprocesoResumen> reprocesar(
            @RequestParam(defaultValue = "true") boolean notificar
    ) {
        return ResponseEntity.ok(reprocesoService.reprocesar(notificar));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}