package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.CompraPublica;
import com.vidayoung.platform.Model.Entity.ProductoDescuentoCliente;
import com.vidayoung.platform.Model.Entity.TipoClientePublico;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.TiendaPublicaService;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TiendaPublicaRestController {

    private final TiendaPublicaService tiendaPublicaService;

    @GetMapping("/api/public/tiendas/{username}")
    public ResponseEntity<TiendaDistribuidorResponse> obtenerTienda(@PathVariable String username) {
        return tiendaPublicaService.buscarDistribuidor(username)
                .map(usuario -> ResponseEntity.ok(TiendaDistribuidorResponse.from(usuario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/public/tiendas/{username}/productos")
    public ResponseEntity<List<TiendaPublicaService.ProductoPublicoResponse>> listarProductos(
            @PathVariable String username,
            String tipoCliente
    ) {
        return ResponseEntity.ok(tiendaPublicaService.listarProductosPublicos(username, tipoCliente));
    }

    @GetMapping("/api/public/tipos-cliente")
    public ResponseEntity<List<TipoClientePublico>> listarTiposCliente() {
        return ResponseEntity.ok(tiendaPublicaService.listarTiposCliente());
    }

    @PostMapping("/api/public/tiendas/{username}/compras")
    public ResponseEntity<CompraPublica> registrarCompraPublica(
            @PathVariable String username,
            @RequestBody TiendaPublicaService.CompraPublicaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaPublicaService.registrarCompraPublica(username, request));
    }

    @GetMapping("/api/compras-publicas")
    public ResponseEntity<List<CompraPublica>> listarComprasPublicas() {
        return ResponseEntity.ok(tiendaPublicaService.listarComprasPublicas());
    }

    @PutMapping("/api/compras-publicas/{compraId}/estado")
    public ResponseEntity<CompraPublica> cambiarEstado(
            @PathVariable Long compraId,
            @RequestBody EstadoCompraPublicaRequest request,
            Authentication authentication
    ) {
        String usuarioOperacion = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(tiendaPublicaService.cambiarEstado(compraId, request.getEstadoCompra(), usuarioOperacion));
    }

    @GetMapping("/api/tipos-cliente-publico")
    public ResponseEntity<List<TipoClientePublico>> listarTiposClienteAdmin() {
        return ResponseEntity.ok(tiendaPublicaService.listarTiposCliente());
    }

    @GetMapping("/api/tipos-cliente-publico/{tipoClienteId}/descuentos")
    public ResponseEntity<List<ProductoDescuentoCliente>> listarDescuentos(@PathVariable Long tipoClienteId) {
        return ResponseEntity.ok(tiendaPublicaService.listarDescuentos(tipoClienteId));
    }

    @PostMapping("/api/productos/{productoId}/descuentos-cliente")
    public ResponseEntity<ProductoDescuentoCliente> guardarDescuento(
            @PathVariable Long productoId,
            @RequestBody DescuentoClienteRequest request
    ) {
        return ResponseEntity.ok(tiendaPublicaService.guardarDescuento(
                productoId,
                request.getTipoClienteId(),
                request.getDescuentoMonto()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    public record TiendaDistribuidorResponse(
            String username,
            Long personaId,
            String nombres,
            String apellidos,
            String fotoPerfil
    ) {
        static TiendaDistribuidorResponse from(Usuario usuario) {
            return new TiendaDistribuidorResponse(
                    usuario.getUsername(),
                    usuario.getPersona().getId(),
                    usuario.getPersona().getNombres(),
                    usuario.getPersona().getApellidos(),
                    usuario.getFotoPerfil()
            );
        }
    }

    @Getter
    @Setter
    public static class EstadoCompraPublicaRequest {

        private String estadoCompra;
    }

    @Getter
    @Setter
    public static class DescuentoClienteRequest {

        private Long tipoClienteId;

        private BigDecimal descuentoMonto;
    }
}
