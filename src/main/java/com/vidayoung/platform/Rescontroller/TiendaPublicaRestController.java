package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.CompraPublica;
import com.vidayoung.platform.Model.Entity.ProductoDescuentoCliente;
import com.vidayoung.platform.Model.Entity.TipoClientePublico;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.TiendaPublicaService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class TiendaPublicaRestController {

    private static final Path PAYMENT_UPLOAD_DIR = Paths.get("uploads", "comprobantes");
    private static final Set<String> COMPROBANTE_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/pdf"
    );

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

    @GetMapping("/api/public/clientes/documento/{documento}")
    public ResponseEntity<TiendaPublicaService.ClientePublicoResponse> buscarClientePorDocumento(@PathVariable String documento) {
        return tiendaPublicaService.buscarClientePorDocumento(documento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/public/tiendas/{username}/clientes/documento/{documento}")
    public ResponseEntity<TiendaPublicaService.ClientePublicoResponse> buscarClientePorDocumentoEnTienda(
            @PathVariable String username,
            @PathVariable String documento
    ) {
        return tiendaPublicaService.buscarClientePorDocumento(username, documento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/public/tiendas/{username}/compras")
    public ResponseEntity<CompraPublica> registrarCompraPublica(
            @PathVariable String username,
            @RequestBody TiendaPublicaService.CompraPublicaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaPublicaService.registrarCompraPublica(username, request));
    }

    @PostMapping(value = "/api/public/tiendas/{username}/compras/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompraPublica> registrarCompraPublicaConComprobante(
            @PathVariable String username,
            @RequestPart("compra") TiendaPublicaService.CompraPublicaRequest request,
            @RequestPart(value = "comprobante", required = false) MultipartFile comprobante
    ) throws IOException {
        ComprobanteGuardado comprobanteGuardado = guardarComprobante(comprobante);
        TiendaPublicaService.CompraPublicaRequest requestConComprobante = new TiendaPublicaService.CompraPublicaRequest(
                request.items(),
                request.tipoClienteCodigo(),
                request.clienteNombres(),
                request.clienteApellidos(),
                request.clienteDocumento(),
                request.clienteEmail(),
                request.clienteTelefono(),
                request.envioRequiere(),
                request.envioDireccion(),
                request.envioCiudad(),
                request.envioReferencia(),
                request.metodoPago(),
                request.referenciaPago(),
                comprobanteGuardado == null ? null : comprobanteGuardado.url(),
                comprobanteGuardado == null ? null : comprobanteGuardado.nombreOriginal(),
                comprobanteGuardado == null ? null : comprobanteGuardado.contentType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaPublicaService.registrarCompraPublica(username, requestConComprobante));
    }

    @GetMapping("/api/compras-publicas")
    public ResponseEntity<List<CompraPublica>> listarComprasPublicas(@RequestParam(required = false) Long periodoId) {
        return ResponseEntity.ok(tiendaPublicaService.listarComprasPublicasPorPeriodo(periodoId));
    }

    @GetMapping("/api/clientes-publicos")
    public ResponseEntity<List<TiendaPublicaService.ClientePublicoAdminResponse>> listarClientesPublicos(
            @RequestParam(required = false) Long distribuidorId,
            @RequestParam(required = false) Long tipoClienteId
    ) {
        return ResponseEntity.ok(tiendaPublicaService.listarClientesPublicos(distribuidorId, tipoClienteId));
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

    private ComprobanteGuardado guardarComprobante(MultipartFile comprobante) throws IOException {
        if (comprobante == null || comprobante.isEmpty()) {
            return null;
        }

        String contentType = comprobante.getContentType();
        if (contentType == null || !COMPROBANTE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("El comprobante debe ser imagen PNG/JPG/WEBP o PDF.");
        }

        Files.createDirectories(PAYMENT_UPLOAD_DIR);
        String extension = StringUtils.getFilenameExtension(comprobante.getOriginalFilename());
        String safeExtension = extension == null ? "pdf" : extension.toLowerCase(Locale.ROOT);
        String fileName = "comprobante-publico-" + UUID.randomUUID() + "." + safeExtension;
        Path destino = PAYMENT_UPLOAD_DIR.resolve(fileName).normalize();
        comprobante.transferTo(destino);

        return new ComprobanteGuardado(
                "/uploads/comprobantes/" + fileName,
                comprobante.getOriginalFilename(),
                contentType
        );
    }

    private record ComprobanteGuardado(String url, String nombreOriginal, String contentType) {
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
