package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.Asistente.AsistenteChatRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteChatResponse;
import com.vidayoung.platform.Dto.Asistente.AsistenteConfigRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteConfigResponse;
import com.vidayoung.platform.Model.Service.AsistenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AsistenteRestController {

    private final AsistenteService asistenteService;

    @PostMapping("/api/asistente/chat")
    public ResponseEntity<AsistenteChatResponse> chat(@RequestBody AsistenteChatRequest request) {
        return ResponseEntity.ok(asistenteService.enviarMensaje(request));
    }

    @GetMapping("/api/asistente/config")
    public ResponseEntity<AsistenteConfigResponse> obtenerConfiguracion() {
        return ResponseEntity.ok(asistenteService.obtenerConfiguracion());
    }

    @PutMapping("/api/asistente/config")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AsistenteConfigResponse> guardarConfiguracion(@RequestBody AsistenteConfigRequest request) {
        return ResponseEntity.ok(asistenteService.guardarConfiguracion(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> manejarErrorServicio(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(exception.getMessage());
    }
}
