package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Notificacion;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.NotificacionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionRestController {

    private final NotificacionService notificacionService;
    private final UsuarioDao usuarioDao;

    @GetMapping
    public ResponseEntity<List<Notificacion>> listarMias(@AuthenticationPrincipal UserDetails userDetails) {
        return personaIdDe(userDetails)
                .map(personaId -> ResponseEntity.ok(notificacionService.listarMias(personaId)))
                .orElseGet(() -> ResponseEntity.ok(List.of()));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<Long> contarNoLeidas(@AuthenticationPrincipal UserDetails userDetails) {
        return personaIdDe(userDetails)
                .map(personaId -> ResponseEntity.ok(notificacionService.contarNoLeidas(personaId)))
                .orElseGet(() -> ResponseEntity.ok(0L));
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<Notificacion> marcarLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return personaIdDe(userDetails)
                .map(personaId -> ResponseEntity.ok(notificacionService.marcarLeida(id, personaId)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/marcar-todas-leidas")
    public ResponseEntity<Long> marcarTodasLeidas(@AuthenticationPrincipal UserDetails userDetails) {
        return personaIdDe(userDetails)
                .map(personaId -> ResponseEntity.ok(notificacionService.marcarTodasLeidas(personaId)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/enviar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Notificacion> enviar(@RequestBody EnviarRequest request) {
        String titulo = request.titulo() == null ? "" : request.titulo().trim();
        String mensaje = request.mensaje() == null ? "" : request.mensaje().trim();

        if (titulo.isBlank() || mensaje.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Notificacion notificacion;
        if (request.destinatarioId() == null) {
            notificacion = notificacionService.notificarBroadcast(request.tipo(), titulo, mensaje, request.link());
        } else {
            notificacion = notificacionService.notificarPersona(
                    request.destinatarioId(), request.tipo(), titulo, mensaje, request.link());
            if (notificacion == null) {
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(notificacion);
    }

    public record EnviarRequest(Long destinatarioId, String tipo, String titulo, String mensaje, String link) {
    }

    private java.util.Optional<Long> personaIdDe(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return java.util.Optional.empty();
        }

        return usuarioDao.findByUsername(userDetails.getUsername())
                .map(Usuario::getPersona)
                .map(persona -> persona.getId());
    }
}