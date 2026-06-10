package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.MenuConfigResponse;
import com.vidayoung.platform.Model.Entity.MenuSistema;
import com.vidayoung.platform.Model.Service.MenuSistemaService;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuSistemaRestController {

    private final MenuSistemaService menuSistemaService;

    @GetMapping("/config")
    public ResponseEntity<MenuConfigResponse> obtenerConfiguracion() {
        return ResponseEntity.ok(menuSistemaService.obtenerConfiguracion());
    }

    @PostMapping
    public ResponseEntity<MenuSistema> guardarMenu(@RequestBody MenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuSistemaService.guardarMenu(toMenu(request, null)));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<MenuSistema> actualizarMenu(@PathVariable String menuId, @RequestBody MenuRequest request) {
        return ResponseEntity.ok(menuSistemaService.guardarMenu(toMenu(request, menuId)));
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> eliminarMenu(@PathVariable String menuId) {
        menuSistemaService.eliminarMenu(menuId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/permissions")
    public ResponseEntity<Void> guardarPermisos(@RequestBody PermissionsRequest request) {
        menuSistemaService.guardarPermisos(request.getPermissions());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    private MenuSistema toMenu(MenuRequest request, String menuId) {
        return MenuSistema.builder()
                .menuId(menuId == null ? request.getId() : menuId)
                .label(request.getLabel())
                .icon(request.getIcon())
                .custom(true)
                .orden(request.getOrden())
                .build();
    }

    @Getter
    @Setter
    public static class MenuRequest {

        private String id;

        private String label;

        private String icon;

        private Integer orden;
    }

    @Getter
    @Setter
    public static class PermissionsRequest {

        private Map<String, List<String>> permissions = Map.of();
    }
}
