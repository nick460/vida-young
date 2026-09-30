package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.EvolutionApiConfig;
import com.vidayoung.platform.Model.Service.EvolutionApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evolution-config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class EvolutionApiConfigRestController {

    private final EvolutionApiService evolutionApiService;

    @GetMapping
    public ResponseEntity<EvolutionApiConfig> obtener() {
        return ResponseEntity.ok(evolutionApiService.obtenerConfig());
    }

    @PutMapping
    public ResponseEntity<EvolutionApiConfig> guardar(@RequestBody EvolutionApiConfig config) {
        return ResponseEntity.ok(evolutionApiService.guardarConfig(config));
    }
}
