package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Service.CompraService;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraRestController {

    private static final Path PAYMENT_UPLOAD_DIR = Paths.get("uploads", "comprobantes");
    private static final Set<String> COMPROBANTE_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/pdf"
    );

    private final CompraService compraService;

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<List<Compra>> listarPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(compraService.listarPorPersona(personaId));
    }

    @GetMapping
    public ResponseEntity<List<Compra>> listarTodas() {
        return ResponseEntity.ok(compraService.listarTodas());
    }

    @GetMapping("/estado/{estadoCompra}")
    public ResponseEntity<List<Compra>> listarPorEstado(@PathVariable String estadoCompra) {
        return ResponseEntity.ok(compraService.listarPorEstado(estadoCompra));
    }

    @PostMapping("/persona/{personaId}")
    public ResponseEntity<CompraResponse> registrarCompra(
            @PathVariable Long personaId,
            @RequestBody CompraRequest request
    ) {
        Compra compra = compraService.registrarCompra(personaId, request.getItems().stream()
                .map(item -> new CompraService.ItemCompraRequest(item.getProductoId(), item.getCantidad()))
                .toList(), toPagoRequest(request, null));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CompraResponse(compra, compraService.listarBeneficiosPorCompra(compra.getId())));
    }

    @PostMapping(value = "/persona/{personaId}/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompraResponse> registrarCompraConComprobante(
            @PathVariable Long personaId,
            @RequestPart("compra") CompraRequest request,
            @RequestPart(value = "comprobante", required = false) MultipartFile comprobante
    ) throws IOException {
        ComprobanteGuardado comprobanteGuardado = guardarComprobante(comprobante);
        Compra compra = compraService.registrarCompra(personaId, request.getItems().stream()
                .map(item -> new CompraService.ItemCompraRequest(item.getProductoId(), item.getCantidad()))
                .toList(), toPagoRequest(request, comprobanteGuardado));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CompraResponse(compra, compraService.listarBeneficiosPorCompra(compra.getId())));
    }

    @GetMapping("/{compraId}/beneficios")
    public ResponseEntity<List<BeneficioActivacionCompra>> listarBeneficios(@PathVariable Long compraId) {
        return ResponseEntity.ok(compraService.listarBeneficiosPorCompra(compraId));
    }

    @PutMapping("/{compraId}/estado")
    public ResponseEntity<Compra> cambiarEstado(
            @PathVariable Long compraId,
            @RequestBody EstadoCompraRequest request,
            Authentication authentication
    ) {
        String usuarioOperacion = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(compraService.cambiarEstado(compraId, request.getEstadoCompra(), usuarioOperacion));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    private CompraService.PagoCompraRequest toPagoRequest(CompraRequest request, ComprobanteGuardado comprobante) {
        return new CompraService.PagoCompraRequest(
                request.getMetodoPago(),
                request.getBancoPago(),
                request.getCuentaPago(),
                request.getCodigoPago(),
                request.getReferenciaPago(),
                comprobante == null ? null : comprobante.url(),
                comprobante == null ? null : comprobante.nombreOriginal(),
                comprobante == null ? null : comprobante.contentType()
        );
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
        String fileName = "comprobante-" + UUID.randomUUID() + "." + safeExtension;
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

    @Getter
    @Setter
    public static class CompraRequest {

        private List<ItemRequest> items = List.of();

        private String metodoPago;

        private String bancoPago;

        private String cuentaPago;

        private String codigoPago;

        private String referenciaPago;
    }

    @Getter
    @Setter
    public static class ItemRequest {

        private Long productoId;

        private Integer cantidad;
    }

    @Getter
    @Setter
    public static class EstadoCompraRequest {

        private String estadoCompra;
    }

    @Getter
    @RequiredArgsConstructor
    public static class CompraResponse {

        private final Compra compra;

        private final List<BeneficioActivacionCompra> beneficios;
    }
}
