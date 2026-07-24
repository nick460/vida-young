package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionNivelDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.CompraDetalle;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import com.vidayoung.platform.Model.Service.CompraService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private static final Set<String> ESTADOS_COMPRA_VALIDOS = Set.of(
            Compra.ESTADO_COMPRA_PENDIENTE,
            Compra.ESTADO_COMPRA_VALIDADA,
            Compra.ESTADO_COMPRA_ENTREGADA,
            Compra.ESTADO_COMPRA_RECHAZADA,
            Compra.ESTADO_COMPRA_CONFIRMADA
    );

    private final CompraDao compraDao;
    private final ProductoDao productoDao;
    private final PersonaDao personaDao;
    private final BilleteraDao billeteraDao;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final ReferidoDao referidoDao;
    private final PlanActivacionDao planActivacionDao;
    private final PlanActivacionNivelDao planActivacionNivelDao;
    private final BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    private final BilleteraService billeteraService;
    private final CarteraEmpresaService carteraEmpresaService;
    private final GestionPeriodoService gestionPeriodoService;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Compra registrarCompra(Long personaId, List<ItemCompraRequest> items, PagoCompraRequest pago) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un producto.");
        }

        Persona comprador = personaDao.findById(personaId)
                .filter(persona -> Auditoria.ESTADO_ACTIVO.equals(persona.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));

        Compra compra = Compra.builder()
                .persona(comprador)
                .periodo(gestionPeriodoService.obtenerPeriodoActivo())
                .fechaCompra(LocalDateTime.now())
                .estadoCompra(Compra.ESTADO_COMPRA_PENDIENTE)
                .metodoPago(normalizarTexto(pago == null ? null : pago.metodoPago()))
                .bancoPago(normalizarTexto(pago == null ? null : pago.bancoPago()))
                .cuentaPago(normalizarTexto(pago == null ? null : pago.cuentaPago()))
                .codigoPago(normalizarTexto(pago == null ? null : pago.codigoPago()))
                .referenciaPago(normalizarTexto(pago == null ? null : pago.referenciaPago()))
                .descuentoMonto(BigDecimal.ZERO)
                .descuentoConcepto(normalizarTexto(pago == null ? null : pago.descuentoConcepto()))
                .comprobantePagoUrl(normalizarTexto(pago == null ? null : pago.comprobantePagoUrl()))
                .comprobantePagoNombre(normalizarTexto(pago == null ? null : pago.comprobantePagoNombre()))
                .comprobantePagoTipo(normalizarTexto(pago == null ? null : pago.comprobantePagoTipo()))
                .subtotal(BigDecimal.ZERO)
                .totalPv(BigDecimal.ZERO)
                .totalQp(BigDecimal.ZERO)
                .totalCr(BigDecimal.ZERO)
                .build();
        compra = compraDao.save(compra);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalPv = BigDecimal.ZERO;
        BigDecimal totalQp = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        int totalProductos = 0;

        for (ItemCompraRequest item : items) {
            int cantidad = item.cantidad() == null ? 0 : item.cantidad();
            if (cantidad < 1) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
            }

            Producto producto = productoDao.findById(item.productoId())
                    .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

            BigDecimal precio = zeroIfNull(producto.getPrecio());
            BigDecimal pv = zeroIfNull(producto.getPv());
            BigDecimal qp = zeroIfNull(producto.getQp());
            BigDecimal cr = zeroIfNull(producto.getCr());
            BigDecimal detalleSubtotal = precio.multiply(BigDecimal.valueOf(cantidad));

            compra.getDetalles().add(CompraDetalle.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(precio)
                    .pvUnitario(pv)
                    .qpUnitario(qp)
                    .crUnitario(cr)
                    .subtotal(detalleSubtotal)
                    .build());

            subtotal = subtotal.add(detalleSubtotal);
            totalPv = totalPv.add(pv.multiply(BigDecimal.valueOf(cantidad)));
            totalQp = totalQp.add(qp.multiply(BigDecimal.valueOf(cantidad)));
            totalCr = totalCr.add(cr.multiply(BigDecimal.valueOf(cantidad)));
            totalProductos += cantidad;
        }

        BigDecimal descuentoMonto = zeroIfNull(pago == null ? null : pago.descuentoMonto());
        if (descuentoMonto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo.");
        }
        if (descuentoMonto.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("El descuento no puede ser mayor al total de la venta.");
        }
        String descuentoConcepto = normalizarTexto(pago == null ? null : pago.descuentoConcepto());
        if (descuentoMonto.compareTo(BigDecimal.ZERO) > 0 && descuentoConcepto == null) {
            throw new IllegalArgumentException("Debe ingresar el concepto del descuento.");
        }

        compra.setSubtotal(subtotal.subtract(descuentoMonto));
        compra.setDescuentoMonto(descuentoMonto);
        compra.setDescuentoConcepto(descuentoConcepto);
        compra.setTotalPv(totalPv);
        compra.setTotalQp(totalQp);
        compra.setTotalCr(totalCr);
        compra = compraDao.save(compra);

        return compra;
    }

    @Override
    public List<Compra> listarPorPersona(Long personaId) {
        return compraDao.findByPersonaIdOrderByFechaCompraDesc(personaId).stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .toList();
    }

    @Override
    public List<Compra> listarPorEstado(String estadoCompra) {
        String estado = normalizarTexto(estadoCompra);
        if (estado == null) {
            return listarTodas();
        }

        return compraDao.findByEstadoCompraOrderByFechaCompraDesc(estado.toUpperCase()).stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .toList();
    }

    @Override
    public List<Compra> listarTodas() {
        return compraDao.findAllByOrderByFechaCompraDesc().stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .toList();
    }

    @Override
    public List<Compra> listarPorPeriodo(Long periodoId) {
        Long periodo = periodoId == null ? gestionPeriodoService.obtenerPeriodoActivo().getId() : periodoId;
        return compraDao.findByPeriodoIdOrderByFechaCompraDesc(periodo).stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .toList();
    }

    @Override
    public List<BeneficioActivacionCompra> listarBeneficiosPorCompra(Long compraId) {
        return beneficioActivacionCompraDao.findByCompraId(compraId).stream()
                .filter(beneficio -> Auditoria.ESTADO_ACTIVO.equals(beneficio.getEstado()))
                .toList();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Compra cambiarEstado(Long compraId, String estadoCompra, String usuarioOperacion) {
        String estado = normalizarTexto(estadoCompra);
        if (estado == null || !ESTADOS_COMPRA_VALIDOS.contains(estado.toUpperCase())) {
            throw new IllegalArgumentException("Estado de compra no valido.");
        }

        Compra compra = compraDao.findById(compraId)
                .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada."));
        String estadoAnterior = compra.getEstadoCompra();
        String estadoNuevo = estado.toUpperCase();
        compra.setEstadoCompra(estadoNuevo);
        registrarAuditoriaEstado(compra, estadoNuevo, usuarioOperacion);
        compra = compraDao.save(compra);

        if (debeProcesarValidacion(estadoAnterior, compra.getEstadoCompra())) {
            procesarCompraValidada(compra);
        }

        return compra;
    }

    private void registrarAuditoriaEstado(Compra compra, String estadoNuevo, String usuarioOperacion) {
        String usuario = normalizarTexto(usuarioOperacion);
        String operador = usuario == null ? "SISTEMA" : usuario;
        LocalDateTime ahora = LocalDateTime.now();

        if (Compra.ESTADO_COMPRA_VALIDADA.equals(estadoNuevo) && compra.getFechaValidacion() == null) {
            compra.setUsuarioValidacion(operador);
            compra.setFechaValidacion(ahora);
        }

        if (Compra.ESTADO_COMPRA_ENTREGADA.equals(estadoNuevo)) {
            if (compra.getFechaValidacion() == null) {
                compra.setUsuarioValidacion(operador);
                compra.setFechaValidacion(ahora);
            }
            compra.setUsuarioEntrega(operador);
            compra.setFechaEntrega(ahora);
        }
    }

    private boolean debeProcesarValidacion(String estadoAnterior, String estadoNuevo) {
        boolean antesProcesada = Compra.ESTADO_COMPRA_VALIDADA.equals(estadoAnterior)
                || Compra.ESTADO_COMPRA_ENTREGADA.equals(estadoAnterior)
                || Compra.ESTADO_COMPRA_CONFIRMADA.equals(estadoAnterior);
        boolean ahoraProcesada = Compra.ESTADO_COMPRA_VALIDADA.equals(estadoNuevo)
                || Compra.ESTADO_COMPRA_ENTREGADA.equals(estadoNuevo)
                || Compra.ESTADO_COMPRA_CONFIRMADA.equals(estadoNuevo);
        return !antesProcesada && ahoraProcesada;
    }

    private void procesarCompraValidada(Compra compra) {
        int totalProductos = compra.getDetalles().stream()
                .map(CompraDetalle::getCantidad)
                .filter(value -> value != null)
                .reduce(0, Integer::sum);

        carteraEmpresaService.registrarIngreso(
                "VENTA_INTERNA",
                compra.getId(),
                zeroIfNull(compra.getSubtotal()),
                "Ingreso por venta interna #" + compra.getId()
        );
        Billetera billeteraComprador = acreditarVolumenComprador(compra.getPersona(), compra, zeroIfNull(compra.getTotalPv()), zeroIfNull(compra.getTotalQp()), zeroIfNull(compra.getTotalCr()));
        billeteraService.activarMembresiaPorPv(compra.getPersona(), billeteraComprador.getSaldoPv(), compra.getPeriodo());

        if (beneficioActivacionCompraDao.findByCompraId(compra.getId()).isEmpty()) {
            generarBeneficiosActivacion(compra, totalProductos);
        }
    }

    private Billetera acreditarVolumenComprador(Persona comprador, Compra compra, BigDecimal totalPv, BigDecimal totalQp, BigDecimal totalCr) {
        Billetera billetera = billeteraService.asegurarBilletera(comprador);

        if (totalPv.compareTo(BigDecimal.ZERO) > 0
                && !movimientoBilleteraDao.existsByReferenciaTipoAndReferenciaIdAndTipo("COMPRA", compra.getId(), MovimientoBilletera.TIPO_PV)) {
            billetera.setSaldoPv(zeroIfNull(billetera.getSaldoPv()).add(totalPv));
            billetera = billeteraDao.save(billetera);
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .periodo(compra.getPeriodo())
                    .tipo(MovimientoBilletera.TIPO_PV)
                    .concepto("PV por compra #" + compra.getId())
                    .referenciaTipo("COMPRA")
                    .referenciaId(compra.getId())
                    .monto(totalPv)
                    .saldoResultado(billetera.getSaldoPv())
                    .build());
        }

        if (totalQp.compareTo(BigDecimal.ZERO) > 0
                && !movimientoBilleteraDao.existsByReferenciaTipoAndReferenciaIdAndTipo("COMPRA", compra.getId(), MovimientoBilletera.TIPO_QP)) {
            billetera.setSaldoQp(zeroIfNull(billetera.getSaldoQp()).add(totalQp));
            billetera = billeteraDao.save(billetera);
            billeteraService.actualizarRangoActual(comprador, billetera.getSaldoQp());
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .periodo(compra.getPeriodo())
                    .tipo(MovimientoBilletera.TIPO_QP)
                    .concepto("QP por compra #" + compra.getId())
                    .referenciaTipo("COMPRA")
                    .referenciaId(compra.getId())
                    .monto(totalQp)
                    .saldoResultado(billetera.getSaldoQp())
                    .build());
        }

        if (totalCr.compareTo(BigDecimal.ZERO) > 0
                && !movimientoBilleteraDao.existsByReferenciaTipoAndReferenciaIdAndTipo("COMPRA", compra.getId(), MovimientoBilletera.TIPO_CR)) {
            billetera.setSaldoCr(zeroIfNull(billetera.getSaldoCr()).add(totalCr));
            billetera = billeteraDao.save(billetera);
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .periodo(compra.getPeriodo())
                    .tipo(MovimientoBilletera.TIPO_CR)
                    .concepto("CR por compra #" + compra.getId())
                    .referenciaTipo("COMPRA")
                    .referenciaId(compra.getId())
                    .monto(totalCr)
                    .saldoResultado(billetera.getSaldoCr())
                    .build());
        }

        return billetera;
    }

    private void generarBeneficiosActivacion(Compra compra, int totalProductos) {
        int maxAlcance = planActivacionDao.findAll().stream()
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .map(PlanActivacion::getNivelesAlcance)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0);

        if (maxAlcance < 1) {
            return;
        }

        Persona beneficiario = referidoDao.findByPersonaId(compra.getPersona().getId())
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .map(Referido::getPatrocinador)
                .orElse(null);
        int nivel = 1;

        while (beneficiario != null && nivel <= maxAlcance) {
            Billetera billetera = billeteraService.asegurarBilletera(beneficiario);
            PlanActivacion plan = obtenerPlanActivacionVigente(billetera, nivel).orElse(null);
            PlanActivacionNivel nivelConfig = plan == null
                    ? null
                    : planActivacionNivelDao.findByPlanActivacionIdAndNumeroNivel(plan.getId(), nivel).orElse(null);
            BigDecimal montoPorProducto = nivelConfig == null ? BigDecimal.ZERO : zeroIfNull(nivelConfig.getMontoPorProducto());
            BigDecimal montoTotal = montoPorProducto.multiply(BigDecimal.valueOf(totalProductos));
            boolean paga = plan != null && montoTotal.compareTo(BigDecimal.ZERO) > 0;

            BeneficioActivacionCompra beneficio = beneficioActivacionCompraDao.save(BeneficioActivacionCompra.builder()
                    .compra(compra)
                    .periodo(compra.getPeriodo())
                    .beneficiario(beneficiario)
                    .planActivacion(plan)
                    .nivelGenerado(nivel)
                    .cantidadProductos(totalProductos)
                    .montoPorProducto(paga ? montoPorProducto : BigDecimal.ZERO)
                    .montoTotal(paga ? montoTotal : BigDecimal.ZERO)
                    .paga(paga)
                    .motivo(paga ? "" : "No corresponde por activacion o nivel del plan")
                    .build());

            if (paga) {
                billetera.setSaldoDinero(zeroIfNull(billetera.getSaldoDinero()).add(montoTotal));
                billetera = billeteraDao.save(billetera);
                movimientoBilleteraDao.save(MovimientoBilletera.builder()
                        .billetera(billetera)
                        .periodo(compra.getPeriodo())
                        .tipo(MovimientoBilletera.TIPO_DINERO)
                        .concepto("Beneficio activacion compra #" + compra.getId() + " nivel " + nivel)
                        .referenciaTipo("BENEFICIO_ACTIVACION_COMPRA")
                        .referenciaId(beneficio.getId())
                        .monto(montoTotal)
                        .saldoResultado(billetera.getSaldoDinero())
                        .build());
            }

            beneficiario = referidoDao.findByPersonaId(beneficiario.getId())
                    .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                    .map(Referido::getPatrocinador)
                    .orElse(null);
            nivel++;
        }
    }

    private Optional<PlanActivacion> obtenerPlanActivacionVigente(Billetera billetera, int nivel) {
        BigDecimal pvMensual = zeroIfNull(billetera.getSaldoPv());

        return planActivacionDao.findByPvMinimoMensualLessThanEqualOrderByPvMinimoMensualDesc(pvMensual).stream()
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .filter(plan -> Optional.ofNullable(plan.getNivelesAlcance()).orElse(0) >= nivel)
                .findFirst();
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
}
