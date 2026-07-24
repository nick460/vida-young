package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDetalleDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import com.vidayoung.platform.Model.Entity.RetiroBilleteraDetalle;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recompensas")
@RequiredArgsConstructor
public class RecompensaRestController {

    private static final String REFERENCIA_RETIRO_NIVEL_1 = "RETIRO_RECOMPENSA_NIVEL_1";

    private final RecompensaDao recompensaDao;
    private final ProductoDao productoDao;
    private final RetiroBilleteraDao retiroBilleteraDao;
    private final RetiroBilleteraDetalleDao retiroBilleteraDetalleDao;
    private final GestionPeriodoService gestionPeriodoService;
    private final CarteraEmpresaService carteraEmpresaService;

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<List<Recompensa>> listarPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .toList());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @GetMapping("/nivel-1")
    public ResponseEntity<List<Recompensa>> listarNivelUnoPendientes(@RequestParam(required = false) Long periodoId) {
        return ResponseEntity.ok(recompensaDao.findAll().stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> Integer.valueOf(1).equals(recompensa.getNivelGenerado()))
                .filter(recompensa -> periodoId == null
                        || (recompensa.getPeriodo() != null && periodoId.equals(recompensa.getPeriodo().getId())))
                .filter(this::tieneSaldoNivelUno)
                .toList());
    }

    @GetMapping("/nivel-1/retiros")
    public ResponseEntity<List<RetiroNivelUnoResponse>> listarRetirosNivelUno(@RequestParam(required = false) Long periodoId) {
        PeriodoGestion periodo = periodoId == null
                ? gestionPeriodoService.obtenerPeriodoActivo()
                : gestionPeriodoService.buscarPorId(periodoId);
        return ResponseEntity.ok(retiroBilleteraDao
                .findByPeriodoIdAndReferenciaTipoWithPersonaOrderByFechaRetiroDesc(periodo.getId(), REFERENCIA_RETIRO_NIVEL_1)
                .stream()
                .map(this::retiroNivelUnoResponse)
                .toList());
    }

    @PostMapping("/nivel-1/{recompensaId}/retiro")
    @Transactional
    public ResponseEntity<RetiroBilletera> registrarRetiroNivelUno(
            @PathVariable Long recompensaId,
            @RequestBody RetiroNivelUnoRequest request
    ) {
        Recompensa recompensa = recompensaDao.findById(recompensaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .filter(item -> Boolean.TRUE.equals(item.getCobrable()))
                .filter(item -> Integer.valueOf(1).equals(item.getNivelGenerado()))
                .orElseThrow(() -> new IllegalArgumentException("Recompensa de nivel 1 no encontrada o no cobrable."));

        BigDecimal dinero = zeroIfNull(request.getMontoDinero());
        List<ProductoRetiroCalculado> productosCalculados = calcularProductos(request.getProductos());
        BigDecimal productos = productosCalculados.isEmpty()
                ? zeroIfNull(request.getMontoProductos())
                : productosCalculados.stream()
                        .map(ProductoRetiroCalculado::subtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (dinero.compareTo(BigDecimal.ZERO) < 0 || productos.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los montos de retiro no pueden ser negativos.");
        }
        if (dinero.add(productos).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ingresa efectivo o productos para retirar.");
        }
        if (efectivoDisponible(recompensa).compareTo(dinero) < 0) {
            throw new IllegalArgumentException("La recompensa no tiene efectivo suficiente.");
        }
        if (productosDisponible(recompensa).compareTo(productos) < 0) {
            throw new IllegalArgumentException("La recompensa no tiene credito de productos suficiente.");
        }

        PeriodoGestion periodo = recompensa.getPeriodo() == null
                ? gestionPeriodoService.obtenerPeriodoActivo()
                : recompensa.getPeriodo();
        RetiroBilletera retiro = retiroBilleteraDao.save(RetiroBilletera.builder()
                .persona(recompensa.getBeneficiario())
                .montoDinero(dinero)
                .montoProductos(productos)
                .estadoRetiro(RetiroBilletera.ESTADO_PROCESADO)
                .fechaRetiro(LocalDateTime.now())
                .observacion(normalizarTexto(request.getObservacion()))
                .periodo(periodo)
                .referenciaTipo(REFERENCIA_RETIRO_NIVEL_1)
                .referenciaId(recompensa.getId())
                .build());

        for (ProductoRetiroCalculado item : productosCalculados) {
            RetiroBilleteraDetalle detalle = retiroBilleteraDetalleDao.save(RetiroBilleteraDetalle.builder()
                    .retiro(retiro)
                    .producto(item.producto())
                    .cantidad(item.cantidad())
                    .precioProveedor(item.precioProveedor())
                    .subtotal(item.subtotal())
                    .build());
            retiro.getDetalles().add(detalle);
        }

        recompensa.setMontoEfectivoRetirado(zeroIfNull(recompensa.getMontoEfectivoRetirado()).add(dinero));
        recompensa.setValorProductosRetirado(zeroIfNull(recompensa.getValorProductosRetirado()).add(productos));
        recompensaDao.save(recompensa);

        if (dinero.compareTo(BigDecimal.ZERO) > 0) {
            carteraEmpresaService.registrarEgreso(
                    REFERENCIA_RETIRO_NIVEL_1,
                    retiro.getId(),
                    dinero,
                    "Retiro inmediato de recompensa nivel 1 #" + recompensa.getId()
            );
        }

        return ResponseEntity.ok(retiro);
    }

    private boolean tieneSaldoNivelUno(Recompensa recompensa) {
        return efectivoDisponible(recompensa).add(productosDisponible(recompensa)).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal efectivoDisponible(Recompensa recompensa) {
        return zeroIfNull(recompensa.getMontoEfectivo())
                .subtract(zeroIfNull(recompensa.getMontoEfectivoRetirado()))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal productosDisponible(Recompensa recompensa) {
        return zeroIfNull(recompensa.getValorProductos())
                .subtract(zeroIfNull(recompensa.getValorProductosRetirado()))
                .max(BigDecimal.ZERO);
    }

    private List<ProductoRetiroCalculado> calcularProductos(List<ProductoRetiroRequest> productos) {
        if (productos == null || productos.isEmpty()) {
            return List.of();
        }

        return productos.stream()
                .filter(item -> item != null && item.getProductoId() != null)
                .map(item -> {
                    int cantidad = item.getCantidad() == null ? 0 : item.getCantidad();
                    if (cantidad < 1) {
                        throw new IllegalArgumentException("La cantidad de productos a retirar debe ser mayor a cero.");
                    }
                    Producto producto = productoDao.findById(item.getProductoId())
                            .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para retiro."));
                    BigDecimal precioProveedor = zeroIfNull(producto.getPrecio());
                    return new ProductoRetiroCalculado(
                            producto,
                            cantidad,
                            precioProveedor,
                            precioProveedor.multiply(BigDecimal.valueOf(cantidad))
                    );
                })
                .toList();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizarTexto(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private RetiroNivelUnoResponse retiroNivelUnoResponse(RetiroBilletera retiro) {
        Recompensa recompensa = retiro.getReferenciaId() == null
                ? null
                : recompensaDao.findById(retiro.getReferenciaId()).orElse(null);
        PeriodoGestion periodo = retiro.getPeriodo();
        return new RetiroNivelUnoResponse(
                retiro.getId(),
                retiro.getReferenciaId(),
                retiro.getPersona() == null ? null : retiro.getPersona().getId(),
                retiro.getPersona() == null ? null : retiro.getPersona().getNombres(),
                retiro.getPersona() == null ? null : retiro.getPersona().getApellidos(),
                retiro.getPersona() == null ? null : retiro.getPersona().getDocumento(),
                retiro.getMontoDinero(),
                retiro.getMontoProductos(),
                retiro.getEstadoRetiro(),
                retiro.getFechaRetiro(),
                periodo == null ? null : periodo.getId(),
                periodo == null ? null : periodo.getNombre(),
                periodo == null || periodo.getGestion() == null ? null : periodo.getGestion().getAnio(),
                recompensa == null || recompensa.getReferido() == null ? null : recompensa.getReferido().getPersona(),
                recompensa == null ? null : recompensa.getPlanIngreso(),
                retiro.getObservacion(),
                retiroBilleteraDetalleDao.findByRetiroId(retiro.getId()).stream()
                        .map(this::retiroDetalleResponse)
                        .toList()
        );
    }

    private RetiroDetalleResponse retiroDetalleResponse(RetiroBilleteraDetalle detalle) {
        return new RetiroDetalleResponse(
                detalle.getProducto() == null ? null : detalle.getProducto().getId(),
                detalle.getProducto() == null ? null : detalle.getProducto().getNombre(),
                detalle.getProducto() == null ? null : detalle.getProducto().getSku(),
                detalle.getCantidad(),
                zeroIfNull(detalle.getPrecioProveedor()),
                zeroIfNull(detalle.getSubtotal())
        );
    }

    private record ProductoRetiroCalculado(Producto producto, Integer cantidad, BigDecimal precioProveedor, BigDecimal subtotal) {
    }

    @Getter
    @Setter
    public static class RetiroNivelUnoRequest {

        private BigDecimal montoDinero;

        private BigDecimal montoProductos;

        private List<ProductoRetiroRequest> productos = List.of();

        private String observacion;
    }

    @Getter
    @Setter
    public static class ProductoRetiroRequest {

        private Long productoId;

        private Integer cantidad;
    }

    @Getter
    @RequiredArgsConstructor
    public static class RetiroNivelUnoResponse {

        private final Long id;
        private final Long recompensaId;
        private final Long personaId;
        private final String nombres;
        private final String apellidos;
        private final String documento;
        private final BigDecimal montoDinero;
        private final BigDecimal montoProductos;
        private final String estadoRetiro;
        private final LocalDateTime fechaRetiro;
        private final Long periodoId;
        private final String periodoNombre;
        private final Integer gestionAnio;
        private final com.vidayoung.platform.Model.Entity.Persona referidoPersona;
        private final com.vidayoung.platform.Model.Entity.Plan planIngreso;
        private final String observacion;
        private final List<RetiroDetalleResponse> detalles;
    }

    @Getter
    @RequiredArgsConstructor
    public static class RetiroDetalleResponse {

        private final Long productoId;
        private final String productoNombre;
        private final String productoSku;
        private final Integer cantidad;
        private final BigDecimal precioProveedor;
        private final BigDecimal subtotal;
    }
}
