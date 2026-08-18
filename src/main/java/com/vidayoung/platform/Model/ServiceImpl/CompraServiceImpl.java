package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.MovimientoCarteraEmpresaDao;
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
import com.vidayoung.platform.Model.Entity.MovimientoCarteraEmpresa;
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
    private static final String REFERENCIA_COMPRA_BONO_REFERIDO = "COMPRA_BONO_REFERIDO";


    private static final Set<String> ESTADOS_COMPRA_VALIDOS = Set.of(
            Compra.ESTADO_COMPRA_PENDIENTE,
            Compra.ESTADO_COMPRA_VALIDADA,
            Compra.ESTADO_COMPRA_RECHAZADA,
            Compra.ESTADO_COMPRA_CONFIRMADA,
            Compra.ESTADO_COMPRA_ANULADA
    );

    private final CompraDao compraDao;
    private final ProductoDao productoDao;
    private final PersonaDao personaDao;
    private final BilleteraDao billeteraDao;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final MovimientoCarteraEmpresaDao movimientoCarteraEmpresaDao;
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
                .totalQpBonoReferido(BigDecimal.ZERO)
                .totalCr(BigDecimal.ZERO)
                .build();
        compra = compraDao.save(compra);

        recalcularCompra(compra, items, pago);

        return compraDao.save(compra);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Compra modificarCompra(Long compraId, List<ItemCompraRequest> items, PagoCompraRequest pago) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un producto.");
        }

        Compra compra = compraDao.findById(compraId)
                .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada."));

        if (!Compra.ESTADO_COMPRA_PENDIENTE.equals(compra.getEstadoCompra())) {
            throw new IllegalArgumentException("Solo se pueden modificar compras pendientes.");
        }

        compra.setMetodoPago(normalizarTexto(pago == null ? null : pago.metodoPago()));
        compra.setBancoPago(normalizarTexto(pago == null ? null : pago.bancoPago()));
        compra.setCuentaPago(normalizarTexto(pago == null ? null : pago.cuentaPago()));
        compra.setCodigoPago(normalizarTexto(pago == null ? null : pago.codigoPago()));
        compra.setReferenciaPago(normalizarTexto(pago == null ? null : pago.referenciaPago()));
        compra.getDetalles().clear();
        recalcularCompra(compra, items, pago);

        return compraDao.save(compra);
    }

    @Override
    public List<Compra> listarPorPersona(Long personaId) {
        return compraDao.findByPersonaIdOrderByFechaCompraDesc(personaId).stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .toList();
    }

    @Override
    public List<Compra> listarPorPersonaYPeriodo(Long personaId, Long periodoId) {
        if (periodoId == null) {
            return listarPorPersona(personaId);
        }

        return compraDao.findByPersonaIdAndPeriodoIdOrderByFechaCompraDesc(personaId, periodoId).stream()
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
        validarCambioEstado(estadoAnterior, estadoNuevo);
        compra.setEstadoCompra(estadoNuevo);
        registrarAuditoriaEstado(compra, estadoNuevo, usuarioOperacion);
        compra = compraDao.save(compra);

        if (debeProcesarValidacion(estadoAnterior, compra.getEstadoCompra())) {
            procesarCompraValidada(compra);
        }

        return compra;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Compra anularCompra(Long compraId, String motivo, String usuarioOperacion) {
        String motivoNormalizado = normalizarTexto(motivo);
        if (motivoNormalizado == null) {
            throw new IllegalArgumentException("El motivo de anulacion es obligatorio.");
        }

        Compra compra = compraDao.findById(compraId)
                .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada."));
        if (!Compra.ESTADO_COMPRA_VALIDADA.equals(compra.getEstadoCompra())) {
            throw new IllegalArgumentException("Solo se pueden anular compras validadas.");
        }

        revertirMovimientosCompra(compra);
        revertirBeneficiosCompra(compra);

        compra.setEstadoCompra(Compra.ESTADO_COMPRA_ANULADA);
        compra.setMotivoAnulacion(motivoNormalizado);
        compra.setUsuarioAnulacion(normalizarTexto(usuarioOperacion));
        compra.setFechaAnulacion(LocalDateTime.now());
        return compraDao.save(compra);
    }

    private void revertirMovimientosCompra(Compra compra) {
        List<MovimientoBilletera> movimientos = movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaId("COMPRA", compra.getId());
        for (MovimientoBilletera movimiento : movimientos) {
            if (!Auditoria.ESTADO_ACTIVO.equals(movimiento.getEstado())) {
                continue;
            }
            Billetera billetera = movimiento.getBilletera();
            BigDecimal saldoAnterior = saldoPorTipo(billetera, movimiento.getTipo());
            BigDecimal nuevoSaldo = saldoAnterior.subtract(zeroIfNull(movimiento.getMonto()));
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("No se puede anular la compra porque el saldo "
                        + movimiento.getTipo() + " ya fue utilizado.");
            }
            asignarSaldoPorTipo(billetera, movimiento.getTipo(), nuevoSaldo);
            billeteraDao.save(billetera);
            movimiento.setEstado(Auditoria.ESTADO_ELIMINADO);
            movimientoBilleteraDao.save(movimiento);
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .tipo(movimiento.getTipo())
                    .periodo(compra.getPeriodo())
                    .concepto("Anulacion de compra #" + compra.getId())
                    .referenciaTipo("ANULACION_COMPRA")
                    .referenciaId(compra.getId())
                    .monto(zeroIfNull(movimiento.getMonto()).negate())
                    .saldoResultado(nuevoSaldo)
                    .build());
        }

        List<MovimientoCarteraEmpresa> movimientosCartera = movimientoCarteraEmpresaDao
                .findByReferenciaTipoAndReferenciaId("VENTA_INTERNA", compra.getId());
        BigDecimal importe = movimientosCartera.stream()
                .filter(movimiento -> Auditoria.ESTADO_ACTIVO.equals(movimiento.getEstado()))
                .map(MovimientoCarteraEmpresa::getMonto)
                .map(this::zeroIfNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (importe.compareTo(BigDecimal.ZERO) > 0) {
            carteraEmpresaService.registrarEgreso(
                    "ANULACION_COMPRA", compra.getId(), importe,
                    "Anulacion de venta interna #" + compra.getId());
            movimientosCartera.stream()
                    .filter(movimiento -> Auditoria.ESTADO_ACTIVO.equals(movimiento.getEstado()))
                    .forEach(movimiento -> {
                        movimiento.setEstado(Auditoria.ESTADO_ELIMINADO);
                        movimientoCarteraEmpresaDao.save(movimiento);
                    });
        }
    }

    private void revertirBeneficiosCompra(Compra compra) {
        for (BeneficioActivacionCompra beneficio : beneficioActivacionCompraDao.findByCompraId(compra.getId())) {
            if (!Auditoria.ESTADO_ACTIVO.equals(beneficio.getEstado())) {
                continue;
            }
            if (Boolean.TRUE.equals(beneficio.getPaga())) {
                for (MovimientoBilletera movimiento : movimientoBilleteraDao
                        .findByReferenciaTipoAndReferenciaId("BENEFICIO_ACTIVACION_COMPRA", beneficio.getId())) {
                    if (!Auditoria.ESTADO_ACTIVO.equals(movimiento.getEstado())) {
                        continue;
                    }
                    Billetera billetera = movimiento.getBilletera();
                    BigDecimal nuevoSaldo = zeroIfNull(billetera.getSaldoDinero())
                            .subtract(zeroIfNull(movimiento.getMonto()));
                    if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("No se puede anular la compra porque un beneficio ya fue utilizado.");
                    }
                    billetera.setSaldoDinero(nuevoSaldo);
                    billeteraDao.save(billetera);
                    movimiento.setEstado(Auditoria.ESTADO_ELIMINADO);
                    movimientoBilleteraDao.save(movimiento);
                    movimientoBilleteraDao.save(MovimientoBilletera.builder()
                            .billetera(billetera)
                            .tipo(MovimientoBilletera.TIPO_DINERO)
                            .periodo(compra.getPeriodo())
                            .concepto("Anulacion de beneficio de compra #" + compra.getId())
                            .referenciaTipo("ANULACION_BENEFICIO_COMPRA")
                            .referenciaId(beneficio.getId())
                            .monto(zeroIfNull(movimiento.getMonto()).negate())
                            .saldoResultado(nuevoSaldo)
                            .build());
                }
            }
            beneficio.setEstado(Auditoria.ESTADO_ELIMINADO);
            beneficio.setMotivo("Anulado por anulacion de compra #" + compra.getId());
            beneficioActivacionCompraDao.save(beneficio);
        }
    }

    private BigDecimal saldoPorTipo(Billetera billetera, String tipo) {
        return switch (tipo) {
            case MovimientoBilletera.TIPO_PV -> zeroIfNull(billetera.getSaldoPv());
            case MovimientoBilletera.TIPO_QP -> zeroIfNull(billetera.getSaldoQp());
            case MovimientoBilletera.TIPO_CR -> zeroIfNull(billetera.getSaldoCr());
            case MovimientoBilletera.TIPO_DINERO -> zeroIfNull(billetera.getSaldoDinero());
            default -> throw new IllegalArgumentException("Tipo de movimiento no reversible: " + tipo);
        };
    }

    private void asignarSaldoPorTipo(Billetera billetera, String tipo, BigDecimal saldo) {
        switch (tipo) {
            case MovimientoBilletera.TIPO_PV -> billetera.setSaldoPv(saldo);
            case MovimientoBilletera.TIPO_QP -> {
                billetera.setSaldoQp(saldo);
                billeteraService.actualizarRangoActual(billetera.getPersona(), saldo);
            }
            case MovimientoBilletera.TIPO_CR -> billetera.setSaldoCr(saldo);
            case MovimientoBilletera.TIPO_DINERO -> billetera.setSaldoDinero(saldo);
            default -> throw new IllegalArgumentException("Tipo de movimiento no reversible: " + tipo);
        }
    }

    private void registrarAuditoriaEstado(Compra compra, String estadoNuevo, String usuarioOperacion) {
        String usuario = normalizarTexto(usuarioOperacion);
        String operador = usuario == null ? "SISTEMA" : usuario;
        LocalDateTime ahora = LocalDateTime.now();

        if (Compra.ESTADO_COMPRA_VALIDADA.equals(estadoNuevo) && compra.getFechaValidacion() == null) {
            compra.setUsuarioValidacion(operador);
            compra.setFechaValidacion(ahora);
        }
    }

    private boolean debeProcesarValidacion(String estadoAnterior, String estadoNuevo) {
        boolean antesProcesada = Compra.ESTADO_COMPRA_VALIDADA.equals(estadoAnterior)
                || Compra.ESTADO_COMPRA_CONFIRMADA.equals(estadoAnterior);
        boolean ahoraProcesada = Compra.ESTADO_COMPRA_VALIDADA.equals(estadoNuevo)
                || Compra.ESTADO_COMPRA_CONFIRMADA.equals(estadoNuevo);
        return !antesProcesada && ahoraProcesada;
    }

    private void validarCambioEstado(String estadoAnterior, String estadoNuevo) {
        if (Compra.ESTADO_COMPRA_ENTREGADA.equals(estadoNuevo)) {
            throw new IllegalArgumentException("La etapa de entrega ya no esta disponible.");
        }
        if (Compra.ESTADO_COMPRA_RECHAZADA.equals(estadoNuevo)
                && !Compra.ESTADO_COMPRA_PENDIENTE.equals(estadoAnterior)) {
            throw new IllegalArgumentException("Solo se pueden rechazar compras pendientes.");
        }
        if (Compra.ESTADO_COMPRA_VALIDADA.equals(estadoNuevo)
                && !Compra.ESTADO_COMPRA_PENDIENTE.equals(estadoAnterior)
                && !Compra.ESTADO_COMPRA_VALIDADA.equals(estadoAnterior)) {
            throw new IllegalArgumentException("Solo se pueden validar compras pendientes.");
        }
    }

    private void recalcularCompra(Compra compra, List<ItemCompraRequest> items, PagoCompraRequest pago) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalPv = BigDecimal.ZERO;
        BigDecimal totalQp = BigDecimal.ZERO;
        BigDecimal totalQpBonoReferido = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;

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
            BigDecimal qpBonoReferido = zeroIfNull(producto.getQpBonoReferido());
            BigDecimal cr = zeroIfNull(producto.getCr());
            BigDecimal detalleSubtotal = precio.multiply(BigDecimal.valueOf(cantidad));

            compra.getDetalles().add(CompraDetalle.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(precio)
                    .pvUnitario(pv)
                    .qpUnitario(qp)
                    .qpBonoReferidoUnitario(qpBonoReferido)
                    .crUnitario(cr)
                    .subtotal(detalleSubtotal)
                    .build());

            subtotal = subtotal.add(detalleSubtotal);
            totalPv = totalPv.add(pv.multiply(BigDecimal.valueOf(cantidad)));
            totalQp = totalQp.add(qp.multiply(BigDecimal.valueOf(cantidad)));
            totalQpBonoReferido = totalQpBonoReferido.add(qpBonoReferido.multiply(BigDecimal.valueOf(cantidad)));
            totalCr = totalCr.add(cr.multiply(BigDecimal.valueOf(cantidad)));
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
        compra.setTotalQpBonoReferido(totalQpBonoReferido);
        compra.setTotalCr(totalCr);
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
        acreditarQpBonoReferido(compra);

        if (beneficioActivacionCompraDao.findByCompraId(compra.getId()).isEmpty()) {
            generarBeneficiosActivacion(compra, totalProductos);
        }
    }

    private void acreditarQpBonoReferido(Compra compra) {
        BigDecimal totalQpBonoReferido = zeroIfNull(compra.getTotalQpBonoReferido());
        if (totalQpBonoReferido.compareTo(BigDecimal.ZERO) <= 0
                || movimientoBilleteraDao.existsByReferenciaTipoAndReferenciaIdAndTipo(
                REFERENCIA_COMPRA_BONO_REFERIDO,
                compra.getId(),
                MovimientoBilletera.TIPO_QP
        )) {
            return;
        }

        Persona patrocinador = referidoDao.findByPersonaId(compra.getPersona().getId())
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .map(Referido::getPatrocinador)
                .orElse(null);
        if (patrocinador == null) {
            return;
        }

        Billetera billetera = billeteraService.asegurarBilletera(patrocinador);
        billetera.setSaldoQp(zeroIfNull(billetera.getSaldoQp()).add(totalQpBonoReferido));
        billetera = billeteraDao.save(billetera);
        billeteraService.actualizarRangoActual(patrocinador, billetera.getSaldoQp());
        movimientoBilleteraDao.save(MovimientoBilletera.builder()
                .billetera(billetera)
                .periodo(compra.getPeriodo())
                .tipo(MovimientoBilletera.TIPO_QP)
                .concepto("QP bono referido por compra #" + compra.getId())
                .referenciaTipo(REFERENCIA_COMPRA_BONO_REFERIDO)
                .referenciaId(compra.getId())
                .monto(totalQpBonoReferido)
                .saldoResultado(billetera.getSaldoQp())
                .build());
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
