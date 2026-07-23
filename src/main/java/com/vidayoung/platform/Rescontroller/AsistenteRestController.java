package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.Asistente.AsistenteChatRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteChatResponse;
import com.vidayoung.platform.Model.Service.AsistenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> manejarErrorServicio(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(exception.getMessage());
    }
}
