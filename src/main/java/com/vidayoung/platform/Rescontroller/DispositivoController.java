package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Dispositivo;
import com.vidayoung.platform.Service.DispositivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dispositivos")
public class DispositivoController {

    @Autowired
    private DispositivoService dispositivoService;

    @PostMapping("/vincular")
    public ResponseEntity<?> vincular(@RequestBody Dispositivo dispositivo) {
        if (dispositivo.getToken() == null || dispositivo.getToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Token es requerido");
        }
        if (dispositivo.getPersona() == null || dispositivo.getPersona().getId() == null) {
            return ResponseEntity.badRequest().body("Persona es requerida");
        }

        Optional<Dispositivo> existente = dispositivoService.findByToken(dispositivo.getToken());
        if (existente.isPresent()) {
            Dispositivo d = existente.get();
            // Si el token ya pertenece a otra persona, desvincularlo o devolver error
            if (d.getPersona() != null && !d.getPersona().getId().equals(dispositivo.getPersona().getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("El token ya está vinculado a otra persona (ID: " + d.getPersona().getId() + ")");
            }
            // Ya está vinculado a la misma persona, solo actualizamos si es necesario
            d.setActivo(true);
            d.setPlataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma() : d.getPlataforma());
            dispositivoService.save(d);
            return ResponseEntity.ok(d);
        }

        Dispositivo nuevo = new Dispositivo();
        nuevo.setToken(dispositivo.getToken());
        if (dispositivo.getPersona() != null) {
            nuevo.setPersona(dispositivo.getPersona());
        } else if (dispositivo.getPersona() != null && dispositivo.getPersona().getId() != null) {
            // Caso de uso: venga el objeto persona con ID
            nuevo.setPersona(dispositivo.getPersona());
        } else {
            // Fallback: crear persona vacía y establecer ID después si es necesario
            // Esto shouldn't happen normalmente - el frontend debe enviar el objeto persona
            nuevo.setPersona(new com.vidayoung.platform.Model.Entity.Persona());
        }
        nuevo.setPlataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma() : "WEB");
        nuevo.setActivo(true);
        dispositivoService.save(nuevo);
        return ResponseEntity.ok(nuevo);
    }

    @GetMapping("/estado")
    public ResponseEntity<?> estado(@RequestParam(required = false) String token) {
        if (token != null && !token.isEmpty()) {
            Optional<Dispositivo> d = dispositivoService.findByToken(token);
            if (d.isPresent()) {
                return ResponseEntity.ok(d.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No hay dispositivo registrado con ese token");
        }
        return ResponseEntity.ok("Sin token proporcionado");
    }

    @PutMapping("/{id}/revincular")
    public ResponseEntity<?> revincular(@PathVariable Long id, @RequestBody Dispositivo dispositivo) {
        Optional<Dispositivo> optional = dispositivoService.findByToken(dispositivo.getToken());
        if (optional.isPresent()) {
            Dispositivo d = optional.get();
            if (d.getId().equals(id)) {
                d.setPlataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma() : d.getPlataforma());
                d.setActivo(true);
                dispositivoService.save(d);
                return ResponseEntity.ok(d);
            }
        }
        return ResponseEntity.badRequest().body("Dispositivo no encontrado o token no pertenece a este ID");
    }
}