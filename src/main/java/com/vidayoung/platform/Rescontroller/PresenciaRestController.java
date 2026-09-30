package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.PresenciaResponse;
import com.vidayoung.platform.Model.Service.PresenciaService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presencia")
@RequiredArgsConstructor
public class PresenciaRestController {

    private final PresenciaService presenciaService;

    @PostMapping("/latido")
    public ResponseEntity<Void> latido(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        String ruta = body == null ? null : body.get("rutaActual");
        presenciaService.registrarLatido(
                userDetails.getUsername(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                ruta
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/salir")
    public ResponseEntity<Void> salir(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            presenciaService.marcarSalida(userDetails.getUsername());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/online")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<PresenciaResponse>> online(@RequestParam(defaultValue = "2") int minutos) {
        return ResponseEntity.ok(presenciaService.listarEnLinea(minutos));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Long>> count(@RequestParam(defaultValue = "2") int minutos) {
        return ResponseEntity.ok(Map.of("online", presenciaService.contarEnLinea(minutos)));
    }
}
