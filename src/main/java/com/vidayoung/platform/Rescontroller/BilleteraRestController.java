package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billeteras")
@RequiredArgsConstructor
public class BilleteraRestController {

    private final BilleteraService billeteraService;
    private final PersonaDao personaDao;
    private final CierreMensualBilleteraDao cierreMensualBilleteraDao;
    private final RecompensaDao recompensaDao;
    private final GestionPeriodoService gestionPeriodoService;

    @GetMapping("/saldos")
    public ResponseEntity<List<BilleteraSaldoResponse>> listarSaldos(@RequestParam(required = false) Long periodoId) {
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        Long periodoConsultaId = periodoId == null ? periodoActivo.getId() : periodoId;
        boolean periodoActivoSeleccionado = periodoConsultaId != null && periodoConsultaId.equals(periodoActivo.getId());

        if (periodoActivoSeleccionado) {
            Map<Long, Billetera> billeterasConSaldo = new LinkedHashMap<>();
            billeteraService.listarBilleterasConSaldos().forEach(billetera ->
                    billeterasConSaldo.put(billetera.getPersona().getId(), billetera)
            );
            recompensaDao.findAll().stream()
                    .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                    .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                    .filter(recompensa -> recompensa.getBeneficiario() != null && recompensa.getBeneficiario().getId() != null)
                    .filter(recompensa -> efectivoMensualDisponible(recompensa.getBeneficiario().getId()).compareTo(BigDecimal.ZERO) > 0)
                    .forEach(recompensa -> billeterasConSaldo.computeIfAbsent(
                            recompensa.getBeneficiario().getId(),
                            id -> billeteraService.asegurarBilletera(recompensa.getBeneficiario())
                    ));

            return ResponseEntity.ok(billeterasConSaldo.values().stream()
                    .map(billetera -> BilleteraSaldoResponse.desdeBilletera(
                            billetera,
                            efectivoRecompensasDisponible(billetera.getPersona().getId()),
                            BigDecimal.ZERO,
                            true
                    ))
                    .toList());
        }

        return ResponseEntity.ok(cierreMensualBilleteraDao.findConSaldosByPeriodoGestionId(periodoConsultaId).stream()
                .map(cierre -> BilleteraSaldoResponse.desdeCierre(cierre, false))
                .toList());
    }

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
                            BigDecimal.ZERO,
                            detalleEfectivoMensual(personaId)
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

        private final List<DetalleEfectivoMensualResponse> detalleEfectivoMensual;
    }

    @Getter
    @RequiredArgsConstructor
    public static class DetalleEfectivoMensualResponse {

        private final Long recompensaId;

        private final Integer nivelGenerado;

        private final BigDecimal montoDisponible;

        private final String planIngreso;

        private final Long referidoId;

        private final String referidoNombre;

        private final String referidoDocumento;
    }

    @Getter
    @RequiredArgsConstructor
    public static class BilleteraSaldoResponse {

        private final Long personaId;

        private final String nombres;

        private final String apellidos;

        private final String documento;

        private final Long billeteraId;

        private final Long cierreId;

        private final BigDecimal saldoDinero;

        private final BigDecimal saldoPv;

        private final BigDecimal saldoQp;

        private final BigDecimal saldoCr;

        private final BigDecimal saldoProductos;

        private final BigDecimal efectivoRecompensasDisponible;

        private final BigDecimal productosRecompensasDisponible;

        private final boolean periodoActivo;

        private final String origen;

        public static BilleteraSaldoResponse desdeBilletera(
                Billetera billetera,
                BigDecimal efectivoRecompensasDisponible,
                BigDecimal productosRecompensasDisponible,
                boolean periodoActivo
        ) {
            Persona persona = billetera.getPersona();
            return new BilleteraSaldoResponse(
                    persona.getId(),
                    persona.getNombres(),
                    persona.getApellidos(),
                    persona.getDocumento(),
                    billetera.getId(),
                    null,
                    zeroIfNullStatic(billetera.getSaldoDinero()),
                    zeroIfNullStatic(billetera.getSaldoPv()),
                    zeroIfNullStatic(billetera.getSaldoQp()),
                    zeroIfNullStatic(billetera.getSaldoCr()),
                    BigDecimal.ZERO,
                    zeroIfNullStatic(efectivoRecompensasDisponible),
                    zeroIfNullStatic(productosRecompensasDisponible),
                    periodoActivo,
                    "BILLETERA_ACTUAL"
            );
        }

        public static BilleteraSaldoResponse desdeCierre(CierreMensualBilletera cierre, boolean periodoActivo) {
            Persona persona = cierre.getPersona();
            return new BilleteraSaldoResponse(
                    persona.getId(),
                    persona.getNombres(),
                    persona.getApellidos(),
                    persona.getDocumento(),
                    null,
                    cierre.getId(),
                    zeroIfNullStatic(cierre.getSaldoDinero()),
                    zeroIfNullStatic(cierre.getSaldoPv()),
                    zeroIfNullStatic(cierre.getSaldoQp()),
                    zeroIfNullStatic(cierre.getSaldoCr()),
                    zeroIfNullStatic(cierre.getSaldoProductos()),
                    BigDecimal.ZERO,
                    zeroIfNullStatic(cierre.getSaldoProductos()),
                    periodoActivo,
                    "CIERRE_MENSUAL"
            );
        }

        private static BigDecimal zeroIfNullStatic(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
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
        return efectivoMensualDisponible(personaId);
    }

    private BigDecimal efectivoMensualDisponible(Long personaId) {
        return recompensasMensualesDisponibles(personaId).stream()
                .map(this::efectivoDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DetalleEfectivoMensualResponse> detalleEfectivoMensual(Long personaId) {
        return recompensasMensualesDisponibles(personaId).stream()
                .map(recompensa -> {
                    Persona referido = recompensa.getReferido() == null ? null : recompensa.getReferido().getPersona();
                    return new DetalleEfectivoMensualResponse(
                            recompensa.getId(),
                            recompensa.getNivelGenerado(),
                            efectivoDisponible(recompensa),
                            recompensa.getPlanIngreso() == null ? null : recompensa.getPlanIngreso().getNombre(),
                            recompensa.getReferido() == null ? null : recompensa.getReferido().getId(),
                            nombreCompleto(referido),
                            referido == null ? null : referido.getDocumento()
                    );
                })
                .toList();
    }

    private List<Recompensa> recompensasMensualesDisponibles(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> java.util.Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) >= 2)
                .filter(recompensa -> efectivoDisponible(recompensa).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private BigDecimal efectivoDisponible(Recompensa recompensa) {
        return zeroIfNull(recompensa.getMontoEfectivo())
                .subtract(zeroIfNull(recompensa.getMontoEfectivoRetirado()))
                .max(BigDecimal.ZERO);
    }

    private String nombreCompleto(Persona persona) {
        if (persona == null) {
            return "Persona";
        }
        return ((persona.getNombres() == null ? "" : persona.getNombres()) + " "
                + (persona.getApellidos() == null ? "" : persona.getApellidos())).trim();
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
