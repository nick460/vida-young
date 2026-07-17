package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import com.vidayoung.platform.Model.Service.BilleteraService;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billeteras")
@RequiredArgsConstructor
public class BilleteraRestController {

    private final BilleteraService billeteraService;
    private final PersonaDao personaDao;
    private final RecompensaDao recompensaDao;

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<BilleteraResumenResponse> resumenPorPersona(@PathVariable Long personaId) {
        return personaDao.findById(personaId)
                .map(persona -> {
                    Billetera billetera = billeteraService.asegurarBilletera(persona);
                    return ResponseEntity.ok(new BilleteraResumenResponse(
                            billetera,
                            billeteraService.listarMovimientos(personaId),
                            billeteraService.listarHistorialMembresias(personaId),
                            billeteraService.listarCierresMensuales(personaId),
                            efectivoRecompensasDisponible(personaId),
                            zeroIfNull(billetera.getSaldoProductos())
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/persona/{personaId}/movimientos")
    public ResponseEntity<List<MovimientoBilletera>> movimientosPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(billeteraService.listarMovimientos(personaId));
    }

    @GetMapping("/persona/{personaId}/membresias")
    public ResponseEntity<List<HistorialMembresia>> membresiasPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(billeteraService.listarHistorialMembresias(personaId));
    }

    @GetMapping("/persona/{personaId}/cierres")
    public ResponseEntity<List<CierreMensualBilletera>> cierresPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(billeteraService.listarCierresMensuales(personaId));
    }

    @PostMapping("/cierres-mensuales")
    public ResponseEntity<Integer> cerrarMesBilleteras() {
        return ResponseEntity.ok(billeteraService.cerrarMesBilleteras());
    }

    @PostMapping("/persona/{personaId}/retiros")
    public ResponseEntity<RetiroBilletera> registrarRetiro(
            @PathVariable Long personaId,
            @RequestBody RetiroRequest request
    ) {
        return ResponseEntity.ok(billeteraService.registrarRetiro(
                personaId,
                request.getMontoDinero(),
                request.getMontoProductos(),
                (request.getProductos() == null ? List.<ProductoRetiroRequest>of() : request.getProductos()).stream()
                        .map(item -> new BilleteraService.ProductoRetiroRequest(item.getProductoId(), item.getCantidad()))
                        .toList(),
                request.getObservacion()
        ));
    }

    @PostMapping("/persona/{personaId}/activaciones")
    public ResponseEntity<HistorialMembresia> registrarActivacion(
            @PathVariable Long personaId,
            @RequestBody ActivacionRequest request
    ) {
        return ResponseEntity.ok(billeteraService.registrarActivacion(personaId, request.getPlanId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @Getter
    @RequiredArgsConstructor
    public static class BilleteraResumenResponse {

        private final Billetera billetera;

        private final List<MovimientoBilletera> movimientos;

        private final List<HistorialMembresia> membresias;

        private final List<CierreMensualBilletera> cierresMensuales;

        private final BigDecimal efectivoRecompensasDisponible;

        private final BigDecimal productosRecompensasDisponible;
    }

    @Getter
    @Setter
    public static class ActivacionRequest {

        private Long planId;
    }

    @Getter
    @Setter
    public static class RetiroRequest {

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

    private BigDecimal efectivoRecompensasDisponible(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .map(recompensa -> zeroIfNull(recompensa.getMontoEfectivo()).subtract(zeroIfNull(recompensa.getMontoEfectivoRetirado())).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal productosRecompensasDisponible(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .map(recompensa -> zeroIfNull(recompensa.getValorProductos()).subtract(zeroIfNull(recompensa.getValorProductosRetirado())).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
