package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.LoginCarouselItem;
import com.vidayoung.platform.Model.Service.LoginCarouselService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoginCarouselRestController {

    private final LoginCarouselService loginCarouselService;

    @GetMapping("/api/public/login-carousel")
    public ResponseEntity<List<LoginCarouselItem>> listarPublico() {
        return ResponseEntity.ok(loginCarouselService.listarActivos());
    }

    @GetMapping("/api/login-carousel")
    public ResponseEntity<List<LoginCarouselItem>> listar() {
        return ResponseEntity.ok(loginCarouselService.listar());
    }

    @PostMapping("/api/login-carousel")
    public ResponseEntity<LoginCarouselItem> guardar(@RequestBody LoginCarouselItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loginCarouselService.guardar(item));
    }

    @PutMapping("/api/login-carousel/{id}")
    public ResponseEntity<LoginCarouselItem> actualizar(@PathVariable Long id, @RequestBody LoginCarouselItem item) {
        return loginCarouselService.buscarPorId(id)
                .map(actual -> {
                    item.setId(id);
                    return ResponseEntity.ok(loginCarouselService.guardar(item));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/login-carousel/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (loginCarouselService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        loginCarouselService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
