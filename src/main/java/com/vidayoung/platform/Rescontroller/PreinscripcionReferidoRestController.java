package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PreinscripcionReferido;
import com.vidayoung.platform.Model.Service.PreinscripcionReferidoService;
import java.security.Principal;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PreinscripcionReferidoRestController {

    private final PreinscripcionReferidoService preinscripcionReferidoService;
    private final PersonaDao personaDao;
    private final UsuarioDao usuarioDao;

    @GetMapping("/api/public/preinscripciones-referidos/patrocinadores/{patrocinadorId}")
    public ResponseEntity<PatrocinadorResponse> patrocinador(@PathVariable Long patrocinadorId) {
        return personaDao.findById(patrocinadorId)
                .filter(persona -> Auditoria.ESTADO_ACTIVO.equals(persona.getEstado()))
                .map(persona -> ResponseEntity.ok(toPatrocinadorResponse(persona)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/public/preinscripciones-referidos/patrocinadores/usuario/{username}")
    public ResponseEntity<PatrocinadorResponse> patrocinadorPorUsername(@PathVariable String username) {
        return usuarioDao.findByUsername(username)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .filter(usuario -> Auditoria.ESTADO_ACTIVO.equals(usuario.getEstado()))
                .map(usuario -> usuario.getPersona())
                .filter(persona -> persona != null && Auditoria.ESTADO_ACTIVO.equals(persona.getEstado()))
                .map(persona -> ResponseEntity.ok(toPatrocinadorResponse(persona)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/public/preinscripciones-referidos")
    public ResponseEntity<PreinscripcionReferido> crearPublica(@RequestBody PreinscripcionPublicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preinscripcionReferidoService.crear(
                request.getPatrocinadorId(),
                request.getPlanId(),
                request.getNombres(),
                request.getApellidos(),
                request.getDocumento(),
                request.getTelefono(),
                request.getEmail(),
                request.getUsername(),
                request.getPassword(),
                request.getConfirmPassword()
        ));
    }

    @GetMapping("/api/preinscripciones-referidos")
    public ResponseEntity<List<PreinscripcionReferido>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(preinscripcionReferidoService.listar(estado));
    }

    @PostMapping("/api/preinscripciones-referidos/{id}/validar")
    public ResponseEntity<PreinscripcionReferido> validar(
            @PathVariable Long id,
            @RequestBody ValidacionPreinscripcionRequest request,
            Principal principal
    ) {
        String usuario = principal == null ? "SISTEMA" : principal.getName();
        return ResponseEntity.ok(preinscripcionReferidoService.validar(
                id,
                request.getPatrocinadorId(),
                request.getPlanId(),
                request.getNombres(),
                request.getApellidos(),
                request.getDocumento(),
                request.getTelefono(),
                request.getEmail(),
                usuario
        ));
    }

    @PostMapping("/api/preinscripciones-referidos/{id}/rechazar")
    public ResponseEntity<PreinscripcionReferido> rechazar(
            @PathVariable Long id,
            @RequestBody RechazoPreinscripcionRequest request,
            Principal principal
    ) {
        String usuario = principal == null ? "SISTEMA" : principal.getName();
        return ResponseEntity.ok(preinscripcionReferidoService.rechazar(id, request.getObservacion(), usuario));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    private String nombreCompleto(Persona persona) {
        return ((persona.getNombres() == null ? "" : persona.getNombres()) + " " + (persona.getApellidos() == null ? "" : persona.getApellidos())).trim();
    }

    private PatrocinadorResponse toPatrocinadorResponse(Persona persona) {
        return new PatrocinadorResponse(
                persona.getId(),
                nombreCompleto(persona),
                persona.getDocumento(),
                usuarioDao.findByPersonaId(persona.getId())
                        .map(usuario -> usuario.getFotoPerfil())
                        .orElse(null)
        );
    }

    public record PatrocinadorResponse(Long id, String nombreCompleto, String documento, String fotoPerfil) {
    }

    @Getter
    @Setter
    public static class PreinscripcionPublicaRequest {
        private Long patrocinadorId;
        private Long planId;
        private String nombres;
        private String apellidos;
        private String documento;
        private String telefono;
        private String email;
        private String username;
        private String password;
        private String confirmPassword;
    }

    @Getter
    @Setter
    public static class ValidacionPreinscripcionRequest {
        private Long patrocinadorId;
        private Long planId;
        private String nombres;
        private String apellidos;
        private String documento;
        private String telefono;
        private String email;
    }

    @Getter
    @Setter
    public static class RechazoPreinscripcionRequest {
        private String observacion;
    }
}
