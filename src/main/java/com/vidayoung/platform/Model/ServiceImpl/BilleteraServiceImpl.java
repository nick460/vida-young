package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.HistorialMembresiaDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.RangoDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDetalleDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import com.vidayoung.platform.Model.Entity.RetiroBilleteraDetalle;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BilleteraServiceImpl implements BilleteraService {

    private static final String REFERENCIA_AFILIACION = "REFERIDO_AFILIACION";

    private final BilleteraDao billeteraDao;
    private final CierreMensualBilleteraDao cierreMensualBilleteraDao;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final HistorialMembresiaDao historialMembresiaDao;
    private final PersonaDao personaDao;
    private final PlanDao planDao;
    private final ProductoDao productoDao;
    private final RangoDao rangoDao;
    private final RecompensaDao recompensaDao;
    private final ReferidoDao referidoDao;
    private final CarteraEmpresaService carteraEmpresaService;
    private final GestionPeriodoService gestionPeriodoService;
    private final RetiroBilleteraDao retiroBilleteraDao;
    private final RetiroBilleteraDetalleDao retiroBilleteraDetalleDao;

    @Override
    @Transactional
    public Billetera asegurarBilletera(Persona persona) {
        return billeteraDao.findByPersonaId(persona.getId())
                .orElseGet(() -> billeteraDao.save(Billetera.builder()
                        .persona(persona)
                        .saldoDinero(BigDecimal.ZERO)
                        .saldoPv(BigDecimal.ZERO)
                        .saldoQp(BigDecimal.ZERO)
                        .saldoCr(BigDecimal.ZERO)
                        .saldoProductos(BigDecimal.ZERO)
                        .build()));
    }

    @Override
    public Optional<Billetera> buscarPorPersonaId(Long personaId) {
        return billeteraDao.findByPersonaId(personaId);
    }

    @Override
    public List<Billetera> listarBilleterasConSaldos() {
        return billeteraDao.findAllConSaldos();
    }

    @Override
    public List<MovimientoBilletera> listarMovimientos(Long personaId) {
        return movimientoBilleteraDao.findByBilleteraPersonaIdOrderByFechaRegistroDesc(personaId);
    }

    @Override
    public List<HistorialMembresia> listarHistorialMembresias(Long personaId) {
        return historialMembresiaDao.findByPersonaIdOrderByFechaInicioDesc(personaId);
    }

    @Override
    public List<CierreMensualBilletera> listarCierresMensuales(Long personaId) {
        return cierreMensualBilleteraDao.findByPersonaIdOrderByPeriodoDesc(personaId);
    }

    @Override
    @Transactional
    public void actualizarRangoActual(Persona persona, BigDecimal qpActual) {
        if (persona == null || persona.getId() == null) {
            return;
        }

        Persona persistente = personaDao.findById(persona.getId())
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElse(null);

        if (persistente == null) {
            return;
        }

        Rango rango = rangoAlcanzadoPorQp(qpActual).orElse(null);
        Long rangoActualId = persistente.getRangoActual() == null ? null : persistente.getRangoActual().getId();
        Long nuevoRangoId = rango == null ? null : rango.getId();

        if (!java.util.Objects.equals(rangoActualId, nuevoRangoId)) {
            persistente.setRangoActual(rango);
            personaDao.save(persistente);
        }
    }

    @Override
    @Transactional
    public void registrarAfiliacionInicial(Referido referido) {
        if (referido.getId() == null || referido.getPersona() == null || referido.getPlan() == null) {
            return;
        }

        asegurarBilletera(referido.getPersona());
        Long referenciaId = referido.getId();
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();

        if (!historialMembresiaDao.existsByReferenciaTipoAndReferenciaIdAndTipo(
                REFERENCIA_AFILIACION,
                referenciaId,
                HistorialMembresia.TIPO_AFILIACION
        )) {
            historialMembresiaDao.save(HistorialMembresia.builder()
                    .persona(referido.getPersona())
                    .plan(referido.getPlan())
                    .tipo(HistorialMembresia.TIPO_AFILIACION)
                    .fechaInicio(referido.getFechaInicioMembresia())
                    .fechaFin(referido.getFechaFinMembresia())
                    .precioPlan(zeroIfNull(referido.getPlan().getPrecio()))
                    .qpPlan(zeroIfNull(referido.getPlan().getQp()))
                    .referenciaTipo(REFERENCIA_AFILIACION)
                    .referenciaId(referenciaId)
                    .estadoMembresia(Boolean.TRUE.equals(referido.getMembresiaActiva())
                            ? HistorialMembresia.MEMBRESIA_ACTIVA
                            : HistorialMembresia.MEMBRESIA_VENCIDA)
                    .periodo(periodoActivo)
                    .build());
        }

        BigDecimal qpPlan = zeroIfNull(referido.getPlan().getQp());
        carteraEmpresaService.registrarIngreso(
                REFERENCIA_AFILIACION,
                referenciaId,
                zeroIfNull(referido.getPlan().getPrecio()),
                "Ingreso por afiliacion de " + nombreCompleto(referido.getPersona()) + " al plan " + referido.getPlan().getNombre()
        );
        if (referido.getPatrocinador() != null
                && qpPlan.compareTo(BigDecimal.ZERO) > 0
                && !movimientoBilleteraDao.existsByReferenciaTipoAndReferenciaIdAndTipo(
                REFERENCIA_AFILIACION,
                referenciaId,
                MovimientoBilletera.TIPO_QP
        )) {
            Billetera billetera = asegurarBilletera(referido.getPatrocinador());
            billetera.setSaldoQp(zeroIfNull(billetera.getSaldoQp()).add(qpPlan));
            billetera = billeteraDao.save(billetera);
            actualizarRangoActual(referido.getPatrocinador(), billetera.getSaldoQp());
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .tipo(MovimientoBilletera.TIPO_QP)
                    .concepto("QP por afiliar a " + nombreCompleto(referido.getPersona()) + " al plan " + referido.getPlan().getNombre())
                    .referenciaTipo(REFERENCIA_AFILIACION)
                    .referenciaId(referenciaId)
                    .monto(qpPlan)
                    .saldoResultado(billetera.getSaldoQp())
                    .periodo(periodoActivo)
                    .build());
        }
    }

    @Override
    @Transactional
    public HistorialMembresia registrarActivacion(Long personaId, Long planId) {
        Persona persona = personaDao.findById(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));
        Plan plan = planDao.findById(planId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado."));
        Referido referido = referidoDao.findByPersonaId(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("La persona no esta registrada en la red."));

        Billetera billetera = asegurarBilletera(persona);
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        LocalDateTime fechaInicio = LocalDateTime.now();
        LocalDateTime fechaFin = calcularFechaFinMembresia(fechaInicio);

        referido.setPlan(plan);
        referido.setFechaInicioMembresia(fechaInicio);
        referido.setFechaFinMembresia(fechaFin);
        referido.setMembresiaActiva(true);
        referidoDao.save(referido);

        HistorialMembresia historial = historialMembresiaDao.save(HistorialMembresia.builder()
                .persona(persona)
                .plan(plan)
                .tipo(HistorialMembresia.TIPO_ACTIVACION)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .precioPlan(zeroIfNull(plan.getPrecio()))
                .qpPlan(zeroIfNull(plan.getQp()))
                .estadoMembresia(HistorialMembresia.MEMBRESIA_ACTIVA)
                .periodo(periodoActivo)
                .build());

        carteraEmpresaService.registrarIngreso(
                "MEMBRESIA_ACTIVACION",
                historial.getId(),
                zeroIfNull(plan.getPrecio()),
                "Ingreso por activacion de " + nombreCompleto(persona) + " al plan " + plan.getNombre()
        );

        BigDecimal qpPlan = zeroIfNull(plan.getQp());
        if (qpPlan.compareTo(BigDecimal.ZERO) > 0) {
            billetera.setSaldoQp(zeroIfNull(billetera.getSaldoQp()).add(qpPlan));
            billetera = billeteraDao.save(billetera);
            actualizarRangoActual(persona, billetera.getSaldoQp());
            actualizarRecompensasCobrables(persona, true);
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .tipo(MovimientoBilletera.TIPO_QP)
                    .concepto("QP por activacion al plan " + plan.getNombre())
                    .referenciaTipo("MEMBRESIA_ACTIVACION")
                    .referenciaId(historial.getId())
                    .monto(qpPlan)
                    .saldoResultado(billetera.getSaldoQp())
                    .periodo(periodoActivo)
                    .build());
        }

        return historial;
    }

    @Override
    @Transactional
    public RetiroBilletera registrarRetiro(
            Long personaId,
            BigDecimal montoDinero,
            BigDecimal montoProductos,
            List<ProductoRetiroRequest> productosRetiro,
            String observacion
    ) {
        Persona persona = personaDao.findById(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));
        Billetera billetera = asegurarBilletera(persona);
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        BigDecimal dinero = zeroIfNull(montoDinero);
        List<RetiroProductoCalculado> productosCalculados = calcularProductosRetiro(productosRetiro);
        BigDecimal productos = productosCalculados.isEmpty()
                ? zeroIfNull(montoProductos)
                : productosCalculados.stream()
                        .map(RetiroProductoCalculado::subtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivoRecompensasDisponible = efectivoRecompensasMensualesDisponible(personaId);
        BigDecimal productosRecompensasDisponible = BigDecimal.ZERO;
        BigDecimal efectivoTotalDisponible = zeroIfNull(billetera.getSaldoDinero()).add(efectivoRecompensasDisponible);

        if (dinero.compareTo(BigDecimal.ZERO) < 0 || productos.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los montos de retiro no pueden ser negativos.");
        }
        if (productos.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Los retiros mensuales solo permiten efectivo de recompensas de nivel 2 en adelante.");
        }
        if (dinero.add(productos).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ingresa un monto de efectivo o productos para retirar.");
        }
        if (efectivoTotalDisponible.compareTo(dinero) < 0) {
            throw new IllegalArgumentException("La persona no tiene efectivo suficiente para retirar.");
        }
        if (productosRecompensasDisponible.compareTo(productos) < 0) {
            throw new IllegalArgumentException("La persona no tiene productos canjeables suficientes para retirar.");
        }

        RetiroBilletera retiro = retiroBilleteraDao.save(RetiroBilletera.builder()
                .persona(persona)
                .montoDinero(dinero)
                .montoProductos(productos)
                .estadoRetiro(RetiroBilletera.ESTADO_PROCESADO)
                .fechaRetiro(LocalDateTime.now())
                .observacion(normalizarTexto(observacion))
                .periodo(periodoActivo)
                .build());

        for (RetiroProductoCalculado item : productosCalculados) {
            RetiroBilleteraDetalle detalle = retiroBilleteraDetalleDao.save(RetiroBilleteraDetalle.builder()
                    .retiro(retiro)
                    .producto(item.producto())
                    .cantidad(item.cantidad())
                    .precioProveedor(item.precioProveedor())
                    .subtotal(item.subtotal())
                    .build());
            retiro.getDetalles().add(detalle);
        }

        if (dinero.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal desdeBilletera = dinero.min(zeroIfNull(billetera.getSaldoDinero()));
            BigDecimal desdeRecompensas = dinero.subtract(desdeBilletera);
            if (desdeBilletera.compareTo(BigDecimal.ZERO) > 0) {
                billetera.setSaldoDinero(zeroIfNull(billetera.getSaldoDinero()).subtract(desdeBilletera));
                billetera = billeteraDao.save(billetera);
                movimientoBilleteraDao.save(MovimientoBilletera.builder()
                        .billetera(billetera)
                        .tipo(MovimientoBilletera.TIPO_DINERO)
                        .concepto("Retiro de efectivo #" + retiro.getId())
                        .referenciaTipo("RETIRO_BILLETERA")
                        .referenciaId(retiro.getId())
                        .monto(desdeBilletera.negate())
                        .saldoResultado(billetera.getSaldoDinero())
                        .periodo(periodoActivo)
                        .build());
            }
            retirarEfectivoRecompensas(personaId, desdeRecompensas);
            carteraEmpresaService.registrarEgreso(
                    "RETIRO_BILLETERA",
                    retiro.getId(),
                    dinero,
                    "Retiro de efectivo de " + nombreCompleto(persona)
            );
        }

        if (productos.compareTo(BigDecimal.ZERO) > 0) {
            retirarProductosRecompensas(personaId, productos);
        }

        return retiro;
    }

    private List<RetiroProductoCalculado> calcularProductosRetiro(List<ProductoRetiroRequest> productosRetiro) {
        if (productosRetiro == null || productosRetiro.isEmpty()) {
            return List.of();
        }

        return productosRetiro.stream()
                .filter(item -> item != null && item.productoId() != null)
                .map(item -> {
                    int cantidad = item.cantidad() == null ? 0 : item.cantidad();
                    if (cantidad < 1) {
                        throw new IllegalArgumentException("La cantidad de productos a retirar debe ser mayor a cero.");
                    }
                    Producto producto = productoDao.findById(item.productoId())
                            .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para retiro."));
                    BigDecimal precioProveedor = zeroIfNull(producto.getPrecio());
                    BigDecimal subtotal = precioProveedor.multiply(BigDecimal.valueOf(cantidad));
                    return new RetiroProductoCalculado(producto, cantidad, precioProveedor, subtotal);
                })
                .toList();
    }

    private record RetiroProductoCalculado(
            Producto producto,
            Integer cantidad,
            BigDecimal precioProveedor,
            BigDecimal subtotal
    ) {
    }

    @Override
    @Transactional
    public void sincronizarSaldoProductosRecompensa(Long recompensaId) {
        recompensaDao.findById(recompensaId).ifPresent(this::sincronizarSaldoProductosRecompensa);
    }

    @Override
    @Transactional
    public int vencerHistorialMembresiasExpiradas() {
        List<HistorialMembresia> vencidas = historialMembresiaDao.findByEstadoMembresiaAndFechaFinLessThanEqual(
                HistorialMembresia.MEMBRESIA_ACTIVA,
                LocalDateTime.now()
        );

        vencidas.forEach(historial -> {
            historial.setEstadoMembresia(HistorialMembresia.MEMBRESIA_VENCIDA);
            historialMembresiaDao.save(historial);
            actualizarRecompensasCobrables(historial.getPersona(), false);
        });

        return vencidas.size();
    }

    @Override
    @Transactional
    public int cerrarMesBilleteras() {
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        String periodo = periodoActivo.getGestion().getAnio() + "-" + String.format("%02d", periodoActivo.getMes());
        LocalDateTime fechaCierre = LocalDateTime.now();
        int totalCierres = 0;

        for (Billetera billetera : billeteraDao.findAll()) {
            if (billetera.getPersona() == null
                    || cierreMensualBilleteraDao.existsByPersonaIdAndPeriodo(billetera.getPersona().getId(), periodo)) {
                continue;
            }

            BigDecimal saldoDinero = zeroIfNull(billetera.getSaldoDinero());
            BigDecimal saldoPv = zeroIfNull(billetera.getSaldoPv());
            BigDecimal saldoQp = zeroIfNull(billetera.getSaldoQp());
            BigDecimal saldoCr = zeroIfNull(billetera.getSaldoCr());
            BigDecimal saldoProductos = zeroIfNull(billetera.getSaldoProductos());
            Rango rango = rangoAlcanzadoPorQp(saldoQp).orElse(null);

            CierreMensualBilletera cierre = cierreMensualBilleteraDao.save(CierreMensualBilletera.builder()
                    .persona(billetera.getPersona())
                    .periodo(periodo)
                    .saldoDinero(saldoDinero)
                    .saldoPv(saldoPv)
                    .saldoQp(saldoQp)
                    .saldoCr(saldoCr)
                    .saldoProductos(saldoProductos)
                    .rango(rango)
                    .rangoNombre(rango == null ? null : rango.getNombre())
                    .rangoQpMinimo(rango == null ? null : zeroIfNull(rango.getQpMinimo()))
                    .estadoPlanilla(CierreMensualBilletera.ESTADO_PLANILLA_PENDIENTE)
                    .fechaCierre(fechaCierre)
                    .periodoGestion(periodoActivo)
                    .build());

            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_DINERO, saldoDinero, periodoActivo);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_PV, saldoPv, periodoActivo);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_QP, saldoQp, periodoActivo);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_CR, saldoCr, periodoActivo);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_PRODUCTOS, saldoProductos, periodoActivo);

            billetera.setSaldoDinero(BigDecimal.ZERO);
            billetera.setSaldoPv(BigDecimal.ZERO);
            billetera.setSaldoQp(BigDecimal.ZERO);
            billetera.setSaldoCr(BigDecimal.ZERO);
            billetera.setSaldoProductos(BigDecimal.ZERO);
            billeteraDao.save(billetera);
            actualizarRangoActual(billetera.getPersona(), BigDecimal.ZERO);
            totalCierres++;
        }

        return totalCierres;
    }

    private Optional<Rango> rangoAlcanzadoPorQp(BigDecimal qp) {
        BigDecimal qpActual = zeroIfNull(qp);

        return rangoDao.findAll().stream()
                .filter(rango -> Auditoria.ESTADO_ACTIVO.equals(rango.getEstado()))
                .filter(rango -> qpActual.compareTo(zeroIfNull(rango.getQpMinimo())) >= 0)
                .max(Comparator.comparing(rango -> zeroIfNull(rango.getQpMinimo())));
    }

    private void actualizarRecompensasCobrables(Persona persona, boolean cobrable) {
        if (persona == null || persona.getId() == null) {
            return;
        }

        recompensaDao.findByBeneficiarioId(persona.getId()).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .forEach(recompensa -> {
                    recompensa.setCobrable(cobrable);
                    recompensa.setMotivoNoCobrable(cobrable ? null : "No cobrable porque la membresia no esta activa.");
                    recompensa = recompensaDao.save(recompensa);
                    sincronizarSaldoProductosRecompensa(recompensa);
                });
    }

    private BigDecimal efectivoRecompensasMensualesDisponible(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) >= 2)
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

    private void retirarEfectivoRecompensas(Long personaId, BigDecimal monto) {
        BigDecimal pendiente = zeroIfNull(monto);
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        for (Recompensa recompensa : recompensasMensualesCobrables(personaId)) {
            BigDecimal disponible = zeroIfNull(recompensa.getMontoEfectivo())
                    .subtract(zeroIfNull(recompensa.getMontoEfectivoRetirado()))
                    .max(BigDecimal.ZERO);
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal retirar = disponible.min(pendiente);
            recompensa.setMontoEfectivoRetirado(zeroIfNull(recompensa.getMontoEfectivoRetirado()).add(retirar));
            recompensaDao.save(recompensa);
            pendiente = pendiente.subtract(retirar);
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
        }
    }

    private void retirarProductosRecompensas(Long personaId, BigDecimal monto) {
        BigDecimal pendiente = zeroIfNull(monto);
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        for (Recompensa recompensa : recompensasCobrables(personaId)) {
            BigDecimal disponible = zeroIfNull(recompensa.getValorProductos())
                    .subtract(zeroIfNull(recompensa.getValorProductosRetirado()))
                    .max(BigDecimal.ZERO);
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal retirar = disponible.min(pendiente);
            recompensa.setValorProductosRetirado(zeroIfNull(recompensa.getValorProductosRetirado()).add(retirar));
            recompensa = recompensaDao.save(recompensa);
            sincronizarSaldoProductosRecompensa(recompensa);
            pendiente = pendiente.subtract(retirar);
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
        }
    }

    private void sincronizarSaldoProductosRecompensa(Recompensa recompensa) {
        if (recompensa == null || recompensa.getId() == null || recompensa.getBeneficiario() == null) {
            return;
        }

        BigDecimal objetivo = BigDecimal.ZERO;
        BigDecimal registrado = movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaIdAndTipo("RECOMPENSA_PRODUCTOS", recompensa.getId(), MovimientoBilletera.TIPO_PRODUCTOS)
                .stream()
                .map(MovimientoBilletera::getMonto)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferencia = objetivo.subtract(registrado);
        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Billetera billetera = asegurarBilletera(recompensa.getBeneficiario());
        PeriodoGestion periodoActivo = recompensa.getPeriodo() == null
                ? gestionPeriodoService.obtenerPeriodoActivo()
                : recompensa.getPeriodo();
        billetera.setSaldoProductos(zeroIfNull(billetera.getSaldoProductos()).add(diferencia));
        billetera = billeteraDao.save(billetera);
        movimientoBilleteraDao.save(MovimientoBilletera.builder()
                .billetera(billetera)
                .tipo(MovimientoBilletera.TIPO_PRODUCTOS)
                .concepto(diferencia.compareTo(BigDecimal.ZERO) > 0
                        ? "Productos canjeables por recompensa #" + recompensa.getId()
                        : "Ajuste de productos por recompensa #" + recompensa.getId())
                .referenciaTipo("RECOMPENSA_PRODUCTOS")
                .referenciaId(recompensa.getId())
                .monto(diferencia)
                .saldoResultado(billetera.getSaldoProductos())
                .periodo(periodoActivo)
                .build());
    }

    private List<Recompensa> recompensasCobrables(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .toList();
    }

    private List<Recompensa> recompensasMensualesCobrables(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) >= 2)
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

    private String nombreCompleto(Persona persona) {
        if (persona == null) {
            return "persona";
        }

        String nombres = persona.getNombres() == null ? "" : persona.getNombres();
        String apellidos = persona.getApellidos() == null ? "" : persona.getApellidos();
        String nombreCompleto = (nombres + " " + apellidos).trim();
        return nombreCompleto.isBlank() ? "persona" : nombreCompleto;
    }

    private void registrarMovimientoCierreSiAplica(
            Billetera billetera,
            CierreMensualBilletera cierre,
            String tipo,
            BigDecimal saldo,
            PeriodoGestion periodo
    ) {
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        movimientoBilleteraDao.save(MovimientoBilletera.builder()
                .billetera(billetera)
                .tipo(tipo)
                .concepto("Cierre mensual " + cierre.getPeriodo())
                .referenciaTipo("CIERRE_MENSUAL")
                .referenciaId(cierre.getId())
                .monto(saldo.negate())
                .saldoResultado(BigDecimal.ZERO)
                .periodo(periodo)
                .build());
    }

    private LocalDateTime calcularFechaFinMembresia(LocalDateTime fechaInicio) {
        LocalDate fechaFin = fechaInicio.toLocalDate().plusMonths(1);
        return LocalDateTime.of(fechaFin, LocalTime.of(23, 59, 59));
    }
}
