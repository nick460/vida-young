package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Service.PersonaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaRestController {

    private final PersonaService personaService;

    @GetMapping
    public ResponseEntity<List<Persona>> listar() {
        return ResponseEntity.ok(personaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Persona> buscarPorId(@PathVariable Long id) {
        return personaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/documento/{documento}")
    public ResponseEntity<Persona> buscarPorDocumento(@PathVariable String documento) {
        return personaService.buscarPorDocumento(documento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Persona> guardar(@RequestBody Persona persona) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personaService.guardar(persona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Persona> actualizar(@PathVariable Long id, @RequestBody Persona persona) {
        return personaService.buscarPorId(id)
                .map(personaActual -> {
                    personaActual.setNombres(persona.getNombres());
                    personaActual.setApellidos(persona.getApellidos());
                    personaActual.setDocumento(persona.getDocumento());
                    personaActual.setEmail(persona.getEmail());
                    personaActual.setTelefono(persona.getTelefono());
                    return ResponseEntity.ok(personaService.guardar(personaActual));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (personaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
