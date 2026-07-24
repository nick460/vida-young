package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDetalleDao;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.CompraDetalle;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import com.vidayoung.platform.Model.Entity.RetiroBilleteraDetalle;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final CompraDao compraDao;
    private final CierreMensualBilleteraDao cierreMensualBilleteraDao;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final RecompensaDao recompensaDao;
    private final RetiroBilleteraDao retiroBilleteraDao;
    private final RetiroBilleteraDetalleDao retiroBilleteraDetalleDao;
    private final GestionPeriodoService gestionPeriodoService;

    @GetMapping("/saldos")
    public ResponseEntity<List<BilleteraSaldoResponse>> listarSaldos(@RequestParam(required = false) Long periodoId) {
        PeriodoGestion periodoActivo = gestionPeriodoService.buscarPeriodoActivo().orElse(null);
        Long periodoConsultaId = periodoId == null ? (periodoActivo == null ? null : periodoActivo.getId()) : periodoId;
        if (periodoConsultaId == null) {
            return ResponseEntity.ok(List.of());
        }
        PeriodoGestion periodoConsulta = gestionPeriodoService.buscarPorId(periodoConsultaId);
        boolean periodoActivoSeleccionado = periodoActivo != null && periodoConsultaId.equals(periodoActivo.getId());
        Map<Long, PeriodoSaldo> saldosPeriodo = new LinkedHashMap<>();

        movimientoBilleteraDao.findByPeriodoIdWithPersona(periodoConsultaId).forEach(movimiento -> {
            Billetera billetera = movimiento.getBilletera();
            Persona persona = billetera.getPersona();
            if (persona == null || persona.getId() == null || retiroBilleteraDao.existsByPersonaIdAndPeriodoId(persona.getId(), periodoConsultaId)) {
                return;
            }
            PeriodoSaldo saldo = saldosPeriodo.computeIfAbsent(persona.getId(), id -> new PeriodoSaldo(persona, billetera.getId()));
            saldo.agregar(movimiento.getTipo(), zeroIfNull(movimiento.getMonto()));
        });

        recompensaDao.findAll().stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> recompensa.getBeneficiario() != null && recompensa.getBeneficiario().getId() != null)
                .filter(recompensa -> recompensa.getPeriodo() != null && periodoConsultaId.equals(recompensa.getPeriodo().getId()))
                .filter(recompensa -> java.util.Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) >= 2)
                .filter(recompensa -> !retiroBilleteraDao.existsByPersonaIdAndPeriodoId(recompensa.getBeneficiario().getId(), periodoConsultaId))
                .forEach(recompensa -> {
                    Billetera billetera = billeteraService.asegurarBilletera(recompensa.getBeneficiario());
                    PeriodoSaldo saldo = saldosPeriodo.computeIfAbsent(recompensa.getBeneficiario().getId(), id -> new PeriodoSaldo(recompensa.getBeneficiario(), billetera.getId()));
                    saldo.efectivoRecompensas = saldo.efectivoRecompensas.add(efectivoDisponible(recompensa));
                });

        return ResponseEntity.ok(saldosPeriodo.values().stream()
                .filter(PeriodoSaldo::tieneSaldo)
                .map(saldo -> BilleteraSaldoResponse.desdePeriodo(saldo, periodoActivoSeleccionado))
                .toList());
    }

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<BilleteraResumenResponse> resumenPorPersona(
            @PathVariable Long personaId,
            @RequestParam(required = false) Long periodoId
    ) {
        return personaDao.findById(personaId)
                .map(persona -> {
                    Billetera billetera = billeteraService.asegurarBilletera(persona);
                    PeriodoGestion periodoConsulta = periodoId == null
                            ? gestionPeriodoService.buscarPeriodoActivo().orElse(null)
                            : gestionPeriodoService.buscarPorId(periodoId);
                    List<MovimientoBilletera> movimientos = periodoConsulta == null
                            ? List.of()
                            : movimientoBilleteraDao.findByBilleteraPersonaIdAndPeriodoIdOrderByFechaRegistroDesc(personaId, periodoConsulta.getId());
                    List<BilleteraMovimientoResponse> movimientosResponse = movimientosResponse(movimientos);
                    return ResponseEntity.ok(new BilleteraResumenResponse(
                            billeteraDesdeMovimientos(billetera, movimientos),
                            movimientosResponse,
                            billeteraService.listarHistorialMembresias(personaId),
                            billeteraService.listarCierresMensuales(personaId),
                            efectivoRecompensasDisponible(personaId, periodoConsulta),
                            BigDecimal.ZERO,
                            detalleEfectivoMensual(personaId, periodoConsulta),
                            efectivoNivel1Disponible(personaId, periodoConsulta),
                            productosNivel1Disponible(personaId, periodoConsulta),
                            periodoConsulta
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

    @GetMapping("/retiros")
    public ResponseEntity<List<RetiroBilleteraResponse>> listarRetiros(@RequestParam(required = false) Long periodoId) {
        PeriodoGestion periodoActivo = gestionPeriodoService.buscarPeriodoActivo().orElse(null);
        Long periodoConsultaId = periodoId == null ? (periodoActivo == null ? null : periodoActivo.getId()) : periodoId;
        if (periodoConsultaId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(retiroBilleteraDao.findByPeriodoIdWithPersonaOrderByFechaRetiroDesc(periodoConsultaId).stream()
                .map(this::retiroResponse)
                .toList());
    }

    @PostMapping("/cierres-mensuales")
    public ResponseEntity<Integer> cerrarMesBilleteras() {
        return ResponseEntity.ok(billeteraService.cerrarMesBilleteras());
    }

    @PostMapping("/cierres-mensuales/cerrar-periodo")
    public ResponseEntity<PeriodoGestion> cerrarPeriodoActivoPagado() {
        return ResponseEntity.ok(billeteraService.cerrarPeriodoActivoPagado());
    }

    @PostMapping("/persona/{personaId}/retiros")
    public ResponseEntity<RetiroBilletera> registrarRetiro(
            @PathVariable Long personaId,
            @RequestBody RetiroRequest request
    ) {
        return ResponseEntity.ok(billeteraService.registrarRetiro(
                personaId,
                request.getPeriodoId(),
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

        private final List<BilleteraMovimientoResponse> movimientos;

        private final List<HistorialMembresia> membresias;

        private final List<CierreMensualBilletera> cierresMensuales;

        private final BigDecimal efectivoRecompensasDisponible;

        private final BigDecimal productosRecompensasDisponible;

        private final List<DetalleEfectivoMensualResponse> detalleEfectivoMensual;

        private final BigDecimal efectivoNivel1Disponible;

        private final BigDecimal productosNivel1Disponible;

        private final PeriodoGestion periodoActivo;
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
    public static class BilleteraMovimientoResponse {

        private final Long id;

        private final String tipo;

        private final String concepto;

        private final String referenciaTipo;

        private final Long referenciaId;

        private final BigDecimal monto;

        private final BigDecimal saldoResultado;

        private final java.time.LocalDateTime fechaRegistro;

        private final CompraMovimientoResponse compra;
    }

    @Getter
    @RequiredArgsConstructor
    public static class CompraMovimientoResponse {

        private final Long id;

        private final java.time.LocalDateTime fechaCompra;

        private final String estadoCompra;

        private final String metodoPago;

        private final BigDecimal subtotal;

        private final BigDecimal totalPv;

        private final BigDecimal totalQp;

        private final BigDecimal totalCr;

        private final List<CompraDetalleMovimientoResponse> detalles;
    }

    @Getter
    @RequiredArgsConstructor
    public static class CompraDetalleMovimientoResponse {

        private final Long productoId;

        private final String productoNombre;

        private final String productoSku;

        private final Integer cantidad;

        private final BigDecimal precioUnitario;

        private final BigDecimal pvUnitario;

        private final BigDecimal qpUnitario;

        private final BigDecimal crUnitario;

        private final BigDecimal subtotal;
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

        public static BilleteraSaldoResponse desdePeriodo(PeriodoSaldo saldo, boolean periodoActivo) {
            Persona persona = saldo.persona;
            return new BilleteraSaldoResponse(
                    persona.getId(),
                    persona.getNombres(),
                    persona.getApellidos(),
                    persona.getDocumento(),
                    saldo.billeteraId,
                    null,
                    zeroIfNullStatic(saldo.saldoDinero).max(BigDecimal.ZERO),
                    zeroIfNullStatic(saldo.saldoPv).max(BigDecimal.ZERO),
                    zeroIfNullStatic(saldo.saldoQp).max(BigDecimal.ZERO),
                    zeroIfNullStatic(saldo.saldoCr).max(BigDecimal.ZERO),
                    zeroIfNullStatic(saldo.saldoProductos).max(BigDecimal.ZERO),
                    zeroIfNullStatic(saldo.efectivoRecompensas).max(BigDecimal.ZERO),
                    BigDecimal.ZERO,
                    periodoActivo,
                    "PERIODO_SELECCIONADO"
            );
        }

        private static BigDecimal zeroIfNullStatic(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private static class PeriodoSaldo {

        private final Persona persona;

        private final Long billeteraId;

        private BigDecimal saldoDinero = BigDecimal.ZERO;

        private BigDecimal saldoPv = BigDecimal.ZERO;

        private BigDecimal saldoQp = BigDecimal.ZERO;

        private BigDecimal saldoCr = BigDecimal.ZERO;

        private BigDecimal saldoProductos = BigDecimal.ZERO;

        private BigDecimal efectivoRecompensas = BigDecimal.ZERO;

        private PeriodoSaldo(Persona persona, Long billeteraId) {
            this.persona = persona;
            this.billeteraId = billeteraId;
        }

        private void agregar(String tipo, BigDecimal monto) {
            BigDecimal valor = monto == null ? BigDecimal.ZERO : monto;
            if (MovimientoBilletera.TIPO_DINERO.equals(tipo)) {
                saldoDinero = saldoDinero.add(valor);
            } else if (MovimientoBilletera.TIPO_PV.equals(tipo)) {
                saldoPv = saldoPv.add(valor);
            } else if (MovimientoBilletera.TIPO_QP.equals(tipo)) {
                saldoQp = saldoQp.add(valor);
            } else if (MovimientoBilletera.TIPO_CR.equals(tipo)) {
                saldoCr = saldoCr.add(valor);
            } else if (MovimientoBilletera.TIPO_PRODUCTOS.equals(tipo)) {
                saldoProductos = saldoProductos.add(valor);
            }
        }

        private boolean tieneSaldo() {
            return saldoDinero.max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0
                    || saldoPv.max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0
                    || saldoQp.max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0
                    || saldoCr.max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0
                    || saldoProductos.max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0
                    || efectivoRecompensas.max(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class RetiroBilleteraResponse {

        private final Long id;

        private final Long personaId;

        private final String nombres;

        private final String apellidos;

        private final String documento;

        private final BigDecimal montoDinero;

        private final BigDecimal montoProductos;

        private final BigDecimal montoDesdeBilletera;

        private final BigDecimal montoDesdeRecompensas;

        private final String estadoRetiro;

        private final java.time.LocalDateTime fechaRetiro;

        private final Long periodoId;

        private final String periodoNombre;

        private final Integer gestionAnio;

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

        private Long periodoId;

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
        return efectivoMensualDisponible(personaId, gestionPeriodoService.buscarPeriodoActivo().orElse(null));
    }

    private BigDecimal efectivoRecompensasDisponible(Long personaId, PeriodoGestion periodo) {
        return efectivoMensualDisponible(personaId, periodo);
    }

    private BigDecimal efectivoMensualDisponible(Long personaId, PeriodoGestion periodo) {
        return recompensasMensualesDisponibles(personaId, periodo).stream()
                .map(this::efectivoDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DetalleEfectivoMensualResponse> detalleEfectivoMensual(Long personaId, PeriodoGestion periodo) {
        return recompensasMensualesDisponibles(personaId, periodo).stream()
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
        return recompensasMensualesDisponibles(personaId, gestionPeriodoService.buscarPeriodoActivo().orElse(null));
    }

    private List<Recompensa> recompensasMensualesDisponibles(Long personaId, PeriodoGestion periodo) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> periodo == null || recompensa.getPeriodo() != null && periodo.getId().equals(recompensa.getPeriodo().getId()))
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

    private BigDecimal efectivoNivel1Disponible(Long personaId, PeriodoGestion periodo) {
        return recompensasNivel1Disponibles(personaId, periodo).stream()
                .map(this::efectivoDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal productosNivel1Disponible(Long personaId, PeriodoGestion periodo) {
        return recompensasNivel1Disponibles(personaId, periodo).stream()
                .map(recompensa -> zeroIfNull(recompensa.getValorProductos())
                        .subtract(zeroIfNull(recompensa.getValorProductosRetirado()))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Recompensa> recompensasNivel1Disponibles(Long personaId, PeriodoGestion periodo) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> periodo == null || recompensa.getPeriodo() != null && periodo.getId().equals(recompensa.getPeriodo().getId()))
                .filter(recompensa -> java.util.Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) == 1)
                .filter(recompensa -> efectivoDisponible(recompensa).compareTo(BigDecimal.ZERO) > 0
                        || zeroIfNull(recompensa.getValorProductos()).subtract(zeroIfNull(recompensa.getValorProductosRetirado())).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private Billetera billeteraDesdeMovimientos(Billetera billetera, List<MovimientoBilletera> movimientos) {
        Billetera reconstruida = new Billetera();
        reconstruida.setId(billetera.getId());
        reconstruida.setPersona(billetera.getPersona());
        reconstruida.setSaldoDinero(ultimoSaldoPorTipo(movimientos, MovimientoBilletera.TIPO_DINERO));
        reconstruida.setSaldoPv(ultimoSaldoPorTipo(movimientos, MovimientoBilletera.TIPO_PV));
        reconstruida.setSaldoQp(ultimoSaldoPorTipo(movimientos, MovimientoBilletera.TIPO_QP));
        reconstruida.setSaldoCr(ultimoSaldoPorTipo(movimientos, MovimientoBilletera.TIPO_CR));
        reconstruida.setSaldoProductos(BigDecimal.ZERO);
        return reconstruida;
    }

    private List<BilleteraMovimientoResponse> movimientosResponse(List<MovimientoBilletera> movimientos) {
        List<Long> compraIds = movimientos.stream()
                .filter(movimiento -> "COMPRA".equals(movimiento.getReferenciaTipo()))
                .map(MovimientoBilletera::getReferenciaId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Compra> comprasPorId = compraIds.isEmpty()
                ? Map.of()
                : compraDao.findByIdInWithDetalles(compraIds).stream()
                        .collect(Collectors.toMap(Compra::getId, Function.identity()));

        return movimientos.stream()
                .map(movimiento -> movimientoResponse(movimiento, comprasPorId.get(movimiento.getReferenciaId())))
                .toList();
    }

    private BilleteraMovimientoResponse movimientoResponse(MovimientoBilletera movimiento, Compra compra) {
        return new BilleteraMovimientoResponse(
                movimiento.getId(),
                movimiento.getTipo(),
                movimiento.getConcepto(),
                movimiento.getReferenciaTipo(),
                movimiento.getReferenciaId(),
                zeroIfNull(movimiento.getMonto()),
                zeroIfNull(movimiento.getSaldoResultado()),
                movimiento.getFechaRegistro(),
                compra == null || !"COMPRA".equals(movimiento.getReferenciaTipo()) ? null : compraMovimientoResponse(compra)
        );
    }

    private CompraMovimientoResponse compraMovimientoResponse(Compra compra) {
        return new CompraMovimientoResponse(
                compra.getId(),
                compra.getFechaCompra(),
                compra.getEstadoCompra(),
                compra.getMetodoPago(),
                zeroIfNull(compra.getSubtotal()),
                zeroIfNull(compra.getTotalPv()),
                zeroIfNull(compra.getTotalQp()),
                zeroIfNull(compra.getTotalCr()),
                compra.getDetalles().stream()
                        .map(this::compraDetalleMovimientoResponse)
                        .toList()
        );
    }

    private CompraDetalleMovimientoResponse compraDetalleMovimientoResponse(CompraDetalle detalle) {
        return new CompraDetalleMovimientoResponse(
                detalle.getProducto() == null ? null : detalle.getProducto().getId(),
                detalle.getProducto() == null ? null : detalle.getProducto().getNombre(),
                detalle.getProducto() == null ? null : detalle.getProducto().getSku(),
                detalle.getCantidad(),
                zeroIfNull(detalle.getPrecioUnitario()),
                zeroIfNull(detalle.getPvUnitario()),
                zeroIfNull(detalle.getQpUnitario()),
                zeroIfNull(detalle.getCrUnitario()),
                zeroIfNull(detalle.getSubtotal())
        );
    }

    private BigDecimal ultimoSaldoPorTipo(List<MovimientoBilletera> movimientos, String tipo) {
        return movimientos.stream()
                .filter(movimiento -> tipo.equals(movimiento.getTipo()))
                .findFirst()
                .map(MovimientoBilletera::getSaldoResultado)
                .map(this::zeroIfNull)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private RetiroBilleteraResponse retiroResponse(RetiroBilletera retiro) {
        Persona persona = retiro.getPersona();
        PeriodoGestion periodo = retiro.getPeriodo();
        BigDecimal desdeBilletera = movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaIdAndTipo("RETIRO_BILLETERA", retiro.getId(), MovimientoBilletera.TIPO_DINERO)
                .stream()
                .map(MovimientoBilletera::getMonto)
                .map(this::zeroIfNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoDinero = zeroIfNull(retiro.getMontoDinero());
        BigDecimal desdeRecompensas = montoDinero.subtract(desdeBilletera).max(BigDecimal.ZERO);
        List<RetiroDetalleResponse> detalles = retiroBilleteraDetalleDao.findByRetiroId(retiro.getId()).stream()
                .map(this::retiroDetalleResponse)
                .toList();

        return new RetiroBilleteraResponse(
                retiro.getId(),
                persona == null ? null : persona.getId(),
                persona == null ? null : persona.getNombres(),
                persona == null ? null : persona.getApellidos(),
                persona == null ? null : persona.getDocumento(),
                montoDinero,
                zeroIfNull(retiro.getMontoProductos()),
                desdeBilletera,
                desdeRecompensas,
                retiro.getEstadoRetiro(),
                retiro.getFechaRetiro(),
                periodo == null ? null : periodo.getId(),
                periodo == null ? null : periodo.getNombre(),
                periodo == null || periodo.getGestion() == null ? null : periodo.getGestion().getAnio(),
                retiro.getObservacion(),
                detalles
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
}
