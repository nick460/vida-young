package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.HistorialMembresiaDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PeriodoGestionDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionNivelDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.RangoDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDao;
import com.vidayoung.platform.Model.Dao.RetiroBilleteraDetalleDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Notificacion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import com.vidayoung.platform.Model.Entity.RetiroBilleteraDetalle;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import com.vidayoung.platform.Model.Service.NotificacionService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BilleteraServiceImpl implements BilleteraService {

    private static final String REFERENCIA_AFILIACION = "REFERIDO_AFILIACION";
    private static final String REFERENCIA_PV_ACTIVACION = "PV_ACTIVACION";
    private static final BigDecimal UMBRAL_REGULA_DIRECTOS = new BigDecimal("25000.00");

    private final BilleteraDao billeteraDao;
    private final CierreMensualBilleteraDao cierreMensualBilleteraDao;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final PeriodoGestionDao periodoGestionDao;
    private final HistorialMembresiaDao historialMembresiaDao;
    private final PersonaDao personaDao;
    private final PlanActivacionDao planActivacionDao;
    private final PlanActivacionNivelDao planActivacionNivelDao;
    private final PlanDao planDao;
    private final ProductoDao productoDao;
    private final RangoDao rangoDao;
    private final RecompensaDao recompensaDao;
    private final ReferidoDao referidoDao;
    private final BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    private final CarteraEmpresaService carteraEmpresaService;
    private final GestionPeriodoService gestionPeriodoService;
    private final RetiroBilleteraDao retiroBilleteraDao;
    private final RetiroBilleteraDetalleDao retiroBilleteraDetalleDao;
    private final NotificacionService notificacionService;

    @Override
    public ProgresoRangosResponse calcularProgresoRangos(Long personaId) {
        Persona persona = personaId == null ? null : personaDao.findById(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElse(null);
        if (persona == null) {
            return new ProgresoRangosResponse(BigDecimal.ZERO, null, null, List.of());
        }

        // QP del mes activo (no acumulado total) — para que el dashboard muestre rango del periodo vigente
        PeriodoGestion periodoActivo = gestionPeriodoService.buscarPeriodoActivo().orElse(null);
        BigDecimal qpTotal;
        Map<Long, BigDecimal> qpPorPersona = new HashMap<>();
        Map<Long, String> nombrePorPersona = new HashMap<>();
        Map<Long, List<Long>> hijosPorPersona = new HashMap<>();

        // Nombres para ramas (independiente del periodo)
        billeteraDao.findAll().forEach(billetera -> {
            if (billetera.getPersona() != null && billetera.getPersona().getId() != null) {
                nombrePorPersona.put(billetera.getPersona().getId(),
                        ((billetera.getPersona().getNombres() == null ? "" : billetera.getPersona().getNombres()) + " "
                                + (billetera.getPersona().getApellidos() == null ? "" : billetera.getPersona().getApellidos())).trim());
            }
        });

        if (periodoActivo != null && periodoActivo.getId() != null) {
            // Sumar QP solo de movimientos ACTIVOS del periodo activo
            movimientoBilleteraDao.findByPeriodoIdWithPersona(periodoActivo.getId()).forEach(mov -> {
                if (!Auditoria.ESTADO_ACTIVO.equals(mov.getEstado()) || !MovimientoBilletera.TIPO_QP.equals(mov.getTipo())) {
                    return;
                }
                if (mov.getBilletera() == null || mov.getBilletera().getPersona() == null || mov.getBilletera().getPersona().getId() == null) {
                    return;
                }
                Long pid = mov.getBilletera().getPersona().getId();
                qpPorPersona.merge(pid, zeroIfNull(mov.getMonto()), BigDecimal::add);
            });
            qpTotal = qpPorPersona.getOrDefault(personaId, BigDecimal.ZERO);
        } else {
            // Fallback sin periodo activo: usar saldo acumulado
            qpTotal = billeteraDao.findByPersonaId(personaId)
                    .map(billetera -> zeroIfNull(billetera.getSaldoQp()))
                    .orElse(BigDecimal.ZERO);
            billeteraDao.findAll().forEach(billetera -> {
                if (billetera.getPersona() != null && billetera.getPersona().getId() != null) {
                    qpPorPersona.put(billetera.getPersona().getId(), zeroIfNull(billetera.getSaldoQp()));
                }
            });
        }

        referidoDao.findAll().stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .filter(referido -> referido.getPatrocinador() != null && referido.getPatrocinador().getId() != null)
                .forEach(referido -> hijosPorPersona
                        .computeIfAbsent(referido.getPatrocinador().getId(), key -> new ArrayList<>())
                        .add(referido.getPersona() == null ? null : referido.getPersona().getId()));

        List<Referido> directos = referidoDao.findByPatrocinadorId(personaId).stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .toList();
        int numeroDirectos = directos.size();

        List<RangoProgresoResponse> rangos = rangoDao.findAll().stream()
                .filter(rango -> Auditoria.ESTADO_ACTIVO.equals(rango.getEstado()))
                .sorted(Comparator.comparing(rango -> zeroIfNull(rango.getQpMinimo())))
                .map(rango -> {
                    boolean reglaDirectos = aplicaReglaDirectos(rango);
                    BigDecimal qpMinimo = zeroIfNull(rango.getQpMinimo());
                    BigDecimal tope = reglaDirectos && numeroDirectos > 0
                            ? qpMinimo.divide(BigDecimal.valueOf(numeroDirectos), 2, RoundingMode.HALF_UP)
                            : null;
                    BigDecimal qpEfectivo;
                    List<RamaProgresoResponse> ramas = new ArrayList<>();
                    if (reglaDirectos) {
                        BigDecimal efectivo = BigDecimal.ZERO;
                        for (Referido directo : directos) {
                            Long raizId = directo.getPersona() == null ? null : directo.getPersona().getId();
                            BigDecimal qpRama = sumaQpSubtree(raizId, hijosPorPersona, qpPorPersona);
                            BigDecimal contable = qpRama.min(tope);
                            efectivo = efectivo.add(contable);
                            ramas.add(new RamaProgresoResponse(
                                    nombrePorPersona.getOrDefault(raizId, "Referido"),
                                    qpRama,
                                    contable));
                        }
                        qpEfectivo = reglaDirectos && numeroDirectos == 0 ? BigDecimal.ZERO : efectivo;
                    } else {
                        qpEfectivo = qpTotal;
                    }
                    boolean cumple = qpEfectivo.compareTo(qpMinimo) >= 0
                            && (!reglaDirectos || numeroDirectos > 0);
                    return new RangoProgresoResponse(
                            rango.getId(),
                            rango.getNombre(),
                            qpMinimo,
                            reglaDirectos,
                            numeroDirectos,
                            tope,
                            qpEfectivo,
                            cumple,
                            ramas
                    );
                })
                .toList();

        String rangoActual = null;
        String rangoSiguiente = null;
        for (RangoProgresoResponse rango : rangos) {
            if (rango.cumple()) {
                rangoActual = rango.nombre();
            } else if (rangoSiguiente == null) {
                rangoSiguiente = rango.nombre();
            }
        }

        return new ProgresoRangosResponse(qpTotal, rangoActual, rangoSiguiente, rangos);
    }

    @Override
    public int calcularAlcanceEfectivo(Persona persona, PlanActivacion plan) {        int base = plan == null || plan.getNivelesAlcance() == null ? 0 : plan.getNivelesAlcance();
        int extra = 0;
        if (persona != null && persona.getRangoActual() != null
                && persona.getRangoActual().getNivelesExtra() != null) {
            extra = persona.getRangoActual().getNivelesExtra();
        }
        return Math.min(NIVELES_TOTALES, base + extra);
    }

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
        return movimientoBilleteraDao.findByBilleteraPersonaIdOrderByFechaRegistroDesc(personaId).stream()
                .filter(movimiento -> Auditoria.ESTADO_ACTIVO.equals(movimiento.getEstado()))
                .toList();
    }

    @Override
    public List<HistorialMembresia> listarHistorialMembresias(Long personaId) {
        return historialMembresiaDao.findByPersonaIdOrderByFechaInicioDesc(personaId).stream()
                .peek(this::hidratarNombreActivacion)
                .toList();
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

        Rango rango = rangoAlcanzadoAplicandoReglas(persistente, qpActual);
        Long rangoActualId = persistente.getRangoActual() == null ? null : persistente.getRangoActual().getId();
        Long nuevoRangoId = rango == null ? null : rango.getId();

        if (!java.util.Objects.equals(rangoActualId, nuevoRangoId)) {
            persistente.setRangoActual(rango);
            personaDao.save(persistente);

            if (rango != null) {
                notificacionService.notificarPersona(
                        persona.getId(),
                        Notificacion.TIPO_RANGO,
                        "Felicitaciones, subiste de rango",
                        "Tu nuevo rango es " + rango.getNombre() + ".",
                        "wallet"
                );
            }
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
            notificacionService.notificarPersona(
                    referido.getPatrocinador().getId(),
                    Notificacion.TIPO_MEMBRESIA,
                    "QP por afiliacion",
                    "Recibiste " + qpPlan + " QP por afiliar a " + nombreCompleto(referido.getPersona()) + " al plan " + referido.getPlan().getNombre() + ".",
                    "wallet"
            );
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
        LocalDateTime fechaInicio = periodoActivo.getFechaInicio().atStartOfDay();
        LocalDateTime fechaFin = finDeDia(periodoActivo.getFechaFin());

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

        notificacionService.notificarPersona(
                persona.getId(),
                Notificacion.TIPO_MEMBRESIA,
                "Membresia activada",
                "Tu membresia al plan " + plan.getNombre() + " fue activada hasta el " + fechaFin.toLocalDate() + ".",
                "wallet"
        );

        recalcularBeneficiosActivacion(persona);

        return historial;
    }

    @Override
    @Transactional
    public void activarMembresiaPorPv(Persona persona, BigDecimal pvActual, PeriodoGestion periodo) {
        if (persona == null || persona.getId() == null || periodo == null) {
            return;
        }

        PlanActivacion planActivacion = obtenerPlanActivacionPorPv(pvActual).orElse(null);
        if (planActivacion == null) {
            return;
        }

        Referido referido = referidoDao.findByPersonaId(persona.getId())
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElse(null);
        if (referido == null || referido.getPlan() == null) {
            return;
        }

        LocalDateTime fechaInicio = periodo.getFechaInicio().atStartOfDay();
        LocalDateTime fechaFin = finDeDia(periodo.getFechaFin());
        if (Boolean.TRUE.equals(referido.getMembresiaActiva())
                && referido.getFechaFinMembresia() != null
                && !referido.getFechaFinMembresia().isBefore(fechaFin)) {
            return;
        }

        referido.setFechaInicioMembresia(fechaInicio);
        referido.setFechaFinMembresia(fechaFin);
        referido.setMembresiaActiva(true);
        referidoDao.save(referido);
        actualizarRecompensasCobrables(persona, true);
        notificacionService.notificarPersona(
                persona.getId(),
                Notificacion.TIPO_MEMBRESIA,
                "Membresia activada por PV",
                "Tu membresia fue activada automaticamente por alcanzar el plan de activacion " + planActivacion.getNombre() + ".",
                "wallet"
        );

        if (historialMembresiaDao.existsByReferenciaTipoAndReferenciaIdAndTipo(
                REFERENCIA_PV_ACTIVACION,
                planActivacion.getId(),
                HistorialMembresia.TIPO_ACTIVACION
        )) {
            return;
        }

        try {
            historialMembresiaDao.save(HistorialMembresia.builder()
                    .persona(persona)
                    .plan(referido.getPlan())
                    .tipo(HistorialMembresia.TIPO_ACTIVACION)
                    .fechaInicio(fechaInicio)
                    .fechaFin(fechaFin)
                    .precioPlan(BigDecimal.ZERO)
                    .qpPlan(BigDecimal.ZERO)
                    .referenciaTipo(REFERENCIA_PV_ACTIVACION)
                    .referenciaId(planActivacion.getId())
                    .estadoMembresia(HistorialMembresia.MEMBRESIA_ACTIVA)
                    .periodo(periodo)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            if (!historialMembresiaDao.existsByReferenciaTipoAndReferenciaIdAndTipo(
                    REFERENCIA_PV_ACTIVACION,
                    planActivacion.getId(),
                    HistorialMembresia.TIPO_ACTIVACION
            )) {
                throw exception;
            }
        }
    }

    @Override
    @Transactional
    public void recalcularBeneficiosActivacion(Persona persona) {
        recalcularBeneficiosActivacion(persona, true);
    }

    @Override
    @Transactional
    public void recalcularBeneficiosActivacion(Persona persona, boolean notificar) {
        if (persona == null || persona.getId() == null) {
            return;
        }

        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        List<BeneficioActivacionCompra> beneficios = beneficioActivacionCompraDao
                .findByBeneficiarioIdAndPeriodoId(persona.getId(), periodoActivo.getId()).stream()
                .filter(beneficio -> Auditoria.ESTADO_ACTIVO.equals(beneficio.getEstado()))
                .toList();
        if (beneficios.isEmpty()) {
            return;
        }

        Billetera billetera = billeteraDao.findByPersonaId(persona.getId()).orElse(null);
        if (billetera == null) {
            return;
        }

        Optional<PlanActivacion> planActivo = obtenerPlanActivacionPorPv(billetera.getSaldoPvPropio());
        boolean membresiaActiva = membresiaActiva(persona, periodoActivo);
        int alcanceEfectivo = calcularAlcanceEfectivo(persona, planActivo.orElse(null));
        int maxNivelConfigurado = planActivo
                .flatMap(plan -> planActivacionNivelDao.findFirstByPlanActivacionIdOrderByNumeroNivelDesc(plan.getId()))
                .map(PlanActivacionNivel::getNumeroNivel)
                .orElse(0);

        for (BeneficioActivacionCompra beneficio : beneficios) {
            Integer nivel = beneficio.getNivelGenerado();
            boolean nivelAplica = nivel != null && nivel >= 1 && nivel <= alcanceEfectivo;
            int numeroConfig = nivel == null ? 0 : Math.min(nivel, Math.max(maxNivelConfigurado, 0));
            PlanActivacionNivel nivelConfig = numeroConfig < 1 || planActivo.isEmpty()
                    ? null
                    : planActivacionNivelDao.findByPlanActivacionIdAndNumeroNivel(
                    planActivo.get().getId(), numeroConfig).orElse(null);
            BigDecimal nuevoMontoPorProducto = nivelConfig == null
                    ? BigDecimal.ZERO
                    : zeroIfNull(nivelConfig.getMontoPorProducto());
            BigDecimal nuevoMontoTotal = nuevoMontoPorProducto
                    .multiply(BigDecimal.valueOf(beneficio.getCantidadProductos()));
            boolean pagaNuevo = planActivo.isPresent()
                    && membresiaActiva
                    && nivelAplica
                    && nuevoMontoTotal.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal montoAnterior = zeroIfNull(beneficio.getMontoTotal());
            BigDecimal diferencia = pagaNuevo
                    ? nuevoMontoTotal.subtract(montoAnterior)
                    : montoAnterior.negate();

            if (diferencia.compareTo(BigDecimal.ZERO) == 0
                    && Boolean.TRUE.equals(beneficio.getPaga()) == pagaNuevo) {
                continue;
            }

            if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                billetera.setSaldoDinero(zeroIfNull(billetera.getSaldoDinero()).add(diferencia));
                billetera = billeteraDao.save(billetera);
            } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal nuevoSaldoDinero = zeroIfNull(billetera.getSaldoDinero()).add(diferencia);
                if (nuevoSaldoDinero.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("No se puede ajustar el beneficio de activacion porque el saldo en dinero ya fue utilizado.");
                }
                billetera.setSaldoDinero(nuevoSaldoDinero);
                billetera = billeteraDao.save(billetera);
            }

            beneficio.setPlanActivacion(planActivo.orElse(null));
            beneficio.setMontoPorProducto(pagaNuevo ? nuevoMontoPorProducto : BigDecimal.ZERO);
            beneficio.setMontoTotal(pagaNuevo ? nuevoMontoTotal : BigDecimal.ZERO);
            beneficio.setPaga(pagaNuevo);
            beneficio.setMotivo(pagaNuevo ? "" : (!nivelAplica
                    ? "No corresponde porque el nivel excede su alcance efectivo"
                    : (membresiaActiva
                    ? "No corresponde por activacion o nivel del plan"
                    : "No corresponde porque la membresia no esta activa")));
            beneficioActivacionCompraDao.save(beneficio);

            if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
                movimientoBilleteraDao.save(MovimientoBilletera.builder()
                        .billetera(billetera)
                        .periodo(periodoActivo)
                        .tipo(MovimientoBilletera.TIPO_DINERO)
                        .concepto("Ajuste retroactivo de beneficio de activacion #" + beneficio.getId()
                                + (diferencia.compareTo(BigDecimal.ZERO) > 0
                                ? " por membresia superior"
                                : " por membresia no activa"))
                        .referenciaTipo("ACTUALIZACION_BENEFICIO_ACTIVACION")
                        .referenciaId(beneficio.getId())
                        .monto(diferencia)
                        .saldoResultado(billetera.getSaldoDinero())
                        .build());
                if (notificar) {
                    notificacionService.notificarPersona(
                            persona.getId(),
                            Notificacion.TIPO_MEMBRESIA,
                            "Actualizacion de beneficios",
                            "Tus beneficios de activacion fueron actualizados segun tu membresia actual.",
                            "wallet"
                    );
                }
            }
        }
    }

    private boolean membresiaActiva(Persona persona, PeriodoGestion periodoActivo) {
        if (persona == null || persona.getId() == null) {
            return false;
        }

        return referidoDao.findByPersonaId(persona.getId())
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .map(referido -> Boolean.TRUE.equals(referido.getMembresiaActiva())
                        && referido.getFechaFinMembresia() != null
                        && !referido.getFechaFinMembresia().toLocalDate().isBefore(periodoActivo.getFechaFin()))
                .orElse(false);
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
        return registrarRetiro(personaId, null, montoDinero, montoProductos, productosRetiro, observacion);
    }

    @Override
    @Transactional
    public RetiroBilletera registrarRetiro(
            Long personaId,
            Long periodoId,
            BigDecimal montoDinero,
            BigDecimal montoProductos,
            List<ProductoRetiroRequest> productosRetiro,
            String observacion
    ) {
        Persona persona = personaDao.findById(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));
        Billetera billetera = asegurarBilletera(persona);
        PeriodoGestion periodoRetiro = periodoId == null
                ? gestionPeriodoService.obtenerPeriodoActivo()
                : gestionPeriodoService.buscarPorId(periodoId);
        BigDecimal dinero = zeroIfNull(montoDinero);
        List<RetiroProductoCalculado> productosCalculados = calcularProductosRetiro(productosRetiro);
        BigDecimal productos = productosCalculados.isEmpty()
                ? zeroIfNull(montoProductos)
                : productosCalculados.stream()
                        .map(RetiroProductoCalculado::subtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivoRecompensasDisponible = efectivoRecompensasMensualesDisponible(personaId, periodoRetiro);
        BigDecimal productosRecompensasDisponible = BigDecimal.ZERO;
        BigDecimal efectivoBilleteraPeriodo = efectivoBilleteraDisponiblePeriodo(personaId, periodoRetiro);
        BigDecimal efectivoTotalDisponible = efectivoBilleteraPeriodo.add(efectivoRecompensasDisponible);

        if (dinero.compareTo(BigDecimal.ZERO) < 0 || productos.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los montos de retiro no pueden ser negativos.");
        }
        if (productos.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Los retiros mensuales solo permiten efectivo de recompensas de nivel 2 en adelante.");
        }
        if (efectivoTotalDisponible.compareTo(dinero) < 0) {
            throw new IllegalArgumentException("La persona no tiene efectivo suficiente para retirar.");
        }
        if (dinero.compareTo(efectivoTotalDisponible) != 0) {
            throw new IllegalArgumentException("El cierre personal debe retirar el total de efectivo disponible.");
        }
        if (productosRecompensasDisponible.compareTo(productos) < 0) {
            throw new IllegalArgumentException("La persona no tiene productos canjeables suficientes para retirar.");
        }

        CierreMensualBilletera cierrePersonal = registrarCierrePersonalPagado(persona, billetera, periodoRetiro);

        RetiroBilletera retiro = retiroBilleteraDao.save(RetiroBilletera.builder()
                .persona(persona)
                .montoDinero(dinero)
                .montoProductos(productos)
                .estadoRetiro(RetiroBilletera.ESTADO_PROCESADO)
                .fechaRetiro(LocalDateTime.now())
                .observacion(normalizarTexto(observacion))
                .periodo(periodoRetiro)
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
            BigDecimal desdeBilletera = dinero.min(efectivoBilleteraPeriodo).min(zeroIfNull(billetera.getSaldoDinero()));
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
                        .periodo(periodoRetiro)
                        .build());
            }
            retirarEfectivoRecompensas(personaId, periodoRetiro, desdeRecompensas);
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

        BigDecimal saldoPvPeriodo = saldoPeriodo(personaId, periodoRetiro, MovimientoBilletera.TIPO_PV);
        BigDecimal saldoQpPeriodo = saldoPeriodo(personaId, periodoRetiro, MovimientoBilletera.TIPO_QP);
        BigDecimal saldoCrPeriodo = saldoPeriodo(personaId, periodoRetiro, MovimientoBilletera.TIPO_CR);
        BigDecimal saldoProductosPeriodo = saldoPeriodo(personaId, periodoRetiro, MovimientoBilletera.TIPO_PRODUCTOS);

        registrarMovimientoCierreSiAplica(billetera, cierrePersonal, MovimientoBilletera.TIPO_PV, saldoPvPeriodo, periodoRetiro);
        registrarMovimientoCierreSiAplica(billetera, cierrePersonal, MovimientoBilletera.TIPO_QP, saldoQpPeriodo, periodoRetiro);
        registrarMovimientoCierreSiAplica(billetera, cierrePersonal, MovimientoBilletera.TIPO_CR, saldoCrPeriodo, periodoRetiro);
        registrarMovimientoCierreSiAplica(billetera, cierrePersonal, MovimientoBilletera.TIPO_PRODUCTOS, saldoProductosPeriodo, periodoRetiro);

        billetera.setSaldoDinero(zeroIfNull(billetera.getSaldoDinero()).max(BigDecimal.ZERO));
        billetera.setSaldoPv(zeroIfNull(billetera.getSaldoPv()).subtract(saldoPvPeriodo).max(BigDecimal.ZERO));
        billetera.setSaldoQp(zeroIfNull(billetera.getSaldoQp()).subtract(saldoQpPeriodo).max(BigDecimal.ZERO));
        billetera.setSaldoCr(zeroIfNull(billetera.getSaldoCr()).subtract(saldoCrPeriodo).max(BigDecimal.ZERO));
        billetera.setSaldoProductos(zeroIfNull(billetera.getSaldoProductos()).subtract(saldoProductosPeriodo).max(BigDecimal.ZERO));
        billeteraDao.save(billetera);
        actualizarRangoActual(persona, billetera.getSaldoQp());

        notificacionService.notificarPersona(
                persona.getId(),
                Notificacion.TIPO_RECOMPENSA,
                "Retiro procesado",
                "Tu retiro #" + retiro.getId() + " por S/ " + dinero + " fue procesado correctamente.",
                "wallet"
        );

        return retiro;
    }

    private CierreMensualBilletera registrarCierrePersonalPagado(Persona persona, Billetera billetera, PeriodoGestion periodoActivo) {
        String periodo = periodoKey(periodoActivo);
        if (cierreMensualBilleteraDao.existsByPersonaIdAndPeriodo(persona.getId(), periodo)) {
            return cierreMensualBilleteraDao.findByPersonaIdOrderByPeriodoDesc(persona.getId()).stream()
                    .filter(cierre -> periodo.equals(cierre.getPeriodo()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El cierre personal no pudo ser encontrado."));
        }

        BigDecimal saldoDinero = efectivoBilleteraDisponiblePeriodo(persona.getId(), periodoActivo);
        BigDecimal saldoPv = saldoPeriodo(persona.getId(), periodoActivo, MovimientoBilletera.TIPO_PV);
        BigDecimal saldoQp = saldoPeriodo(persona.getId(), periodoActivo, MovimientoBilletera.TIPO_QP);
        BigDecimal saldoCr = saldoPeriodo(persona.getId(), periodoActivo, MovimientoBilletera.TIPO_CR);
        BigDecimal saldoProductos = saldoPeriodo(persona.getId(), periodoActivo, MovimientoBilletera.TIPO_PRODUCTOS);
        Rango rango = rangoAlcanzadoPorQp(saldoQp).orElse(null);
        return cierreMensualBilleteraDao.save(CierreMensualBilletera.builder()
                .persona(persona)
                .periodo(periodo)
                .saldoDinero(saldoDinero)
                .saldoPv(saldoPv)
                .saldoQp(saldoQp)
                .saldoCr(saldoCr)
                .saldoProductos(saldoProductos)
                .rango(rango)
                .rangoNombre(rango == null ? null : rango.getNombre())
                .rangoQpMinimo(rango == null ? null : zeroIfNull(rango.getQpMinimo()))
                .estadoPlanilla(CierreMensualBilletera.ESTADO_PLANILLA_PAGADA)
                .fechaCierre(LocalDateTime.now())
                .periodoGestion(periodoActivo)
                .build());
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
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        List<HistorialMembresia> vencidas = historialMembresiaDao.findByEstadoMembresiaAndFechaFinLessThanEqual(
                HistorialMembresia.MEMBRESIA_ACTIVA,
                periodoActivo.getFechaInicio().atStartOfDay()
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
    public int vencerHistorialMembresiasActivas() {
        LocalDateTime finPeriodoActivo = finDeDia(gestionPeriodoService.obtenerPeriodoActivo().getFechaFin());
        List<HistorialMembresia> activas = historialMembresiaDao.findAll().stream()
                .filter(historial -> HistorialMembresia.MEMBRESIA_ACTIVA.equals(historial.getEstadoMembresia()))
                .filter(historial -> Auditoria.ESTADO_ACTIVO.equals(historial.getEstado()))
                .filter(historial -> historial.getFechaFin() != null)
                .filter(historial -> !historial.getFechaFin().isAfter(finPeriodoActivo))
                .toList();

        activas.forEach(historial -> {
            historial.setEstadoMembresia(HistorialMembresia.MEMBRESIA_VENCIDA);
            historialMembresiaDao.save(historial);
            actualizarRecompensasCobrables(historial.getPersona(), false);
        });

        return activas.size();
    }

    @Override
    @Transactional
    public int cerrarMesBilleteras() {
        // Cerrar el periodo que esta pendiente de cierre (el mes que acabo), no el nuevo ACTIVO con movimientos del mes entrante
        PeriodoGestion periodoACerrar = periodoGestionDaoFindPendienteCierreOrActivo();
        String periodo = periodoKey(periodoACerrar);
        LocalDateTime fechaCierre = LocalDateTime.now();
        int totalCierres = 0;

        for (Billetera billetera : billeteraDao.findAll()) {
            if (billetera.getPersona() == null
                    || cierreMensualBilleteraDao.existsByPersonaIdAndPeriodo(billetera.getPersona().getId(), periodo)) {
                continue;
            }

            // Solo lo del mes que se cierra, no todo el saldo (preserva movimientos del nuevo mes activo)
            BigDecimal saldoDinero = efectivoBilleteraDisponiblePeriodo(billetera.getPersona().getId(), periodoACerrar);
            BigDecimal saldoPv = saldoPeriodo(billetera.getPersona().getId(), periodoACerrar, MovimientoBilletera.TIPO_PV);
            BigDecimal saldoQp = saldoPeriodo(billetera.getPersona().getId(), periodoACerrar, MovimientoBilletera.TIPO_QP);
            BigDecimal saldoCr = saldoPeriodo(billetera.getPersona().getId(), periodoACerrar, MovimientoBilletera.TIPO_CR);
            BigDecimal saldoProductos = saldoPeriodo(billetera.getPersona().getId(), periodoACerrar, MovimientoBilletera.TIPO_PRODUCTOS);
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
                    .periodoGestion(periodoACerrar)
                    .build());

            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_DINERO, saldoDinero, periodoACerrar);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_PV, saldoPv, periodoACerrar);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_QP, saldoQp, periodoACerrar);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_CR, saldoCr, periodoACerrar);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_PRODUCTOS, saldoProductos, periodoACerrar);

            // Descontar SOLO lo del mes cerrado, preservando saldo del nuevo mes activo (ej: 1500 -> resta 1500, deja 200)
            billetera.setSaldoDinero(zeroIfNull(billetera.getSaldoDinero()).subtract(saldoDinero).max(BigDecimal.ZERO));
            billetera.setSaldoPv(zeroIfNull(billetera.getSaldoPv()).subtract(saldoPv).max(BigDecimal.ZERO));
            billetera.setSaldoQp(zeroIfNull(billetera.getSaldoQp()).subtract(saldoQp).max(BigDecimal.ZERO));
            billetera.setSaldoCr(zeroIfNull(billetera.getSaldoCr()).subtract(saldoCr).max(BigDecimal.ZERO));
            billetera.setSaldoProductos(zeroIfNull(billetera.getSaldoProductos()).subtract(saldoProductos).max(BigDecimal.ZERO));
            billeteraDao.save(billetera);
            actualizarRangoActual(billetera.getPersona(), billetera.getSaldoQp());
            totalCierres++;
        }

        return totalCierres;
    }

    private PeriodoGestion periodoGestionDaoFindPendienteCierreOrActivo() {
        return periodoGestionDao.findFirstByEstadoPeriodoOrderByFechaInicioDesc(PeriodoGestion.ESTADO_PERIODO_PENDIENTE_CIERRE)
                .orElseGet(gestionPeriodoService::obtenerPeriodoActivo);
    }

    @Override
    @Transactional
    public PeriodoGestion cerrarPeriodoActivoPagado() {
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        if (!listarBilleterasConSaldos().isEmpty()) {
            throw new IllegalArgumentException("Aun existen personas pendientes de cierre personal.");
        }

        boolean existenRecompensasPendientes = recompensaDao.findAll().stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> recompensa.getPeriodo() != null && recompensa.getPeriodo().getId().equals(periodoActivo.getId()))
                .filter(recompensa -> Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) >= 2)
                .anyMatch(recompensa -> zeroIfNull(recompensa.getMontoEfectivo())
                        .subtract(zeroIfNull(recompensa.getMontoEfectivoRetirado()))
                        .max(BigDecimal.ZERO)
                        .compareTo(BigDecimal.ZERO) > 0);
        if (existenRecompensasPendientes) {
            throw new IllegalArgumentException("Aun existen recompensas mensuales pendientes de pago.");
        }

        cerrarMesBilleteras();
        vencerHistorialMembresiasActivas();
        desactivarMembresiasVencidasDelPeriodoActivo();
        return gestionPeriodoService.cerrarPeriodoActivo();
    }

    private Optional<Rango> rangoAlcanzadoPorQp(BigDecimal qp) {
        BigDecimal qpActual = zeroIfNull(qp);

        return rangoDao.findAll().stream()
                .filter(rango -> Auditoria.ESTADO_ACTIVO.equals(rango.getEstado()))
                .filter(rango -> qpActual.compareTo(zeroIfNull(rango.getQpMinimo())) >= 0)
                .max(Comparator.comparing(rango -> zeroIfNull(rango.getQpMinimo())));
    }

    /**
     * Regla de equidad entre ramas: para rangos objetivo con qp_minimo mayor al
     * umbral, el QP contable por cada rama directa se limita a
     * objetivo / numero de directos, y todas las ramas deben aportar.
     */
    private Rango rangoAlcanzadoAplicandoReglas(Persona persona, BigDecimal qpTotal) {
        BigDecimal qp = zeroIfNull(qpTotal);
        List<Rango> activos = rangoDao.findAll().stream()
                .filter(rango -> Auditoria.ESTADO_ACTIVO.equals(rango.getEstado()))
                .sorted(Comparator.comparing((Rango rango) -> zeroIfNull(rango.getQpMinimo())).reversed())
                .toList();

        for (Rango rango : activos) {
            if (qp.compareTo(zeroIfNull(rango.getQpMinimo())) < 0) {
                continue;
            }
            if (aplicaReglaDirectos(rango) && !cumpleReglaDirectos(persona, rango)) {
                continue;
            }
            return rango;
        }
        return null;
    }

    private boolean aplicaReglaDirectos(Rango objetivo) {
        return zeroIfNull(objetivo.getQpMinimo()).compareTo(UMBRAL_REGULA_DIRECTOS) > 0;
    }

    private boolean cumpleReglaDirectos(Persona persona, Rango objetivo) {
        List<Referido> directos = referidoDao.findByPatrocinadorId(persona.getId()).stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .toList();
        int numeroDirectos = directos.size();
        if (numeroDirectos == 0) {
            return false;
        }

        Map<Long, List<Long>> hijosPorPersona = new HashMap<>();
        Map<Long, BigDecimal> qpPorPersona = new HashMap<>();
        referidoDao.findAll().stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .filter(referido -> referido.getPatrocinador() != null && referido.getPatrocinador().getId() != null)
                .forEach(referido -> hijosPorPersona
                        .computeIfAbsent(referido.getPatrocinador().getId(), key -> new ArrayList<>())
                        .add(referido.getPersona() == null ? null : referido.getPersona().getId()));
        billeteraDao.findAll().forEach(billetera -> {
            if (billetera.getPersona() != null && billetera.getPersona().getId() != null) {
                qpPorPersona.put(billetera.getPersona().getId(), zeroIfNull(billetera.getSaldoQp()));
            }
        });

        BigDecimal topePorRama = zeroIfNull(objetivo.getQpMinimo())
                .divide(BigDecimal.valueOf(numeroDirectos), 2, RoundingMode.HALF_UP);
        BigDecimal qpEfectivo = BigDecimal.ZERO;

        for (Referido directo : directos) {
            Long raizId = directo.getPersona() == null ? null : directo.getPersona().getId();
            qpEfectivo = qpEfectivo.add(sumaQpSubtree(raizId, hijosPorPersona, qpPorPersona).min(topePorRama));
        }
        return qpEfectivo.compareTo(zeroIfNull(objetivo.getQpMinimo())) >= 0;
    }

    /** Suma el saldo QP de toda la descendencia de una rama (incluida la raiz). */
    private BigDecimal sumaQpSubtree(Long personaId, Map<Long, List<Long>> hijosPorPersona, Map<Long, BigDecimal> qpPorPersona) {
        BigDecimal total = BigDecimal.ZERO;
        Deque<Long> pendientes = new ArrayDeque<>();
        Set<Long> visitados = new HashSet<>();
        if (personaId != null) {
            pendientes.push(personaId);
            visitados.add(personaId);
        }
        while (!pendientes.isEmpty()) {
            Long actual = pendientes.pop();
            total = total.add(qpPorPersona.getOrDefault(actual, BigDecimal.ZERO));
            for (Long hijo : hijosPorPersona.getOrDefault(actual, List.of())) {
                if (hijo != null && visitados.add(hijo)) {
                    pendientes.push(hijo);
                }
            }
        }
        return total;
    }

    private void desactivarMembresiasVencidasDelPeriodoActivo() {
        LocalDateTime finPeriodoActivo = finDeDia(gestionPeriodoService.obtenerPeriodoActivo().getFechaFin());
        referidoDao.findByMembresiaActivaTrue().stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .filter(referido -> referido.getFechaFinMembresia() != null)
                .filter(referido -> !referido.getFechaFinMembresia().isAfter(finPeriodoActivo))
                .forEach(referido -> {
                    referido.setMembresiaActiva(false);
                    referidoDao.save(referido);
                });
    }

    private String periodoKey(PeriodoGestion periodo) {
        return periodo.getGestion().getAnio() + "-" + String.format("%02d", periodo.getMes());
    }

    private Optional<PlanActivacion> obtenerPlanActivacionPorPv(BigDecimal pvActual) {
        return planActivacionDao.findByPvMinimoMensualLessThanEqualOrderByPvMinimoMensualDesc(zeroIfNull(pvActual)).stream()
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .findFirst();
    }

    private void hidratarNombreActivacion(HistorialMembresia historial) {
        if (historial == null
                || historial.getReferenciaId() == null
                || !"PV_ACTIVACION".equals(historial.getReferenciaTipo())) {
            return;
        }

        planActivacionDao.findById(historial.getReferenciaId())
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .map(PlanActivacion::getNombre)
                .ifPresent(historial::setNombreActivacion);
    }

    private void actualizarRecompensasCobrables(Persona persona, boolean cobrable) {
        if (persona == null || persona.getId() == null) {
            return;
        }

        recompensaDao.findByBeneficiarioId(persona.getId()).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .forEach(recompensa -> {
                    boolean esNivelUno = Integer.valueOf(1).equals(recompensa.getNivelGenerado());
                    boolean cobrableFinal = esNivelUno || cobrable;
                    recompensa.setCobrable(cobrableFinal);
                    recompensa.setMotivoNoCobrable(cobrableFinal ? null : "No cobrable porque la membresia no esta activa.");
                    recompensa = recompensaDao.save(recompensa);
                    sincronizarSaldoProductosRecompensa(recompensa);
                });
    }

    private BigDecimal efectivoRecompensasMensualesDisponible(Long personaId) {
        return efectivoRecompensasMensualesDisponible(personaId, null);
    }

    private BigDecimal efectivoRecompensasMensualesDisponible(Long personaId, PeriodoGestion periodo) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> periodo == null || recompensa.getPeriodo() != null && periodo.getId().equals(recompensa.getPeriodo().getId()))
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
        retirarEfectivoRecompensas(personaId, null, monto);
    }

    private void retirarEfectivoRecompensas(Long personaId, PeriodoGestion periodo, BigDecimal monto) {
        BigDecimal pendiente = zeroIfNull(monto);
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        for (Recompensa recompensa : recompensasMensualesCobrables(personaId, periodo)) {
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

        BigDecimal objetivo = Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()) && Boolean.TRUE.equals(recompensa.getCobrable())
                ? zeroIfNull(recompensa.getValorProductos())
                        .subtract(zeroIfNull(recompensa.getValorProductosRetirado()))
                        .max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        List<MovimientoBilletera> movimientosReferencia = movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaIdAndTipo("RECOMPENSA_PRODUCTOS", recompensa.getId(), MovimientoBilletera.TIPO_PRODUCTOS);
        BigDecimal registrado = movimientosReferencia
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
        if (movimientosReferencia.isEmpty()) {
            movimientoBilleteraDao.save(MovimientoBilletera.builder()
                    .billetera(billetera)
                    .tipo(MovimientoBilletera.TIPO_PRODUCTOS)
                    .concepto("Productos canjeables por recompensa #" + recompensa.getId())
                    .referenciaTipo("RECOMPENSA_PRODUCTOS")
                    .referenciaId(recompensa.getId())
                    .monto(objetivo)
                    .saldoResultado(billetera.getSaldoProductos())
                    .periodo(periodoActivo)
                    .build());
            return;
        }

        MovimientoBilletera movimiento = movimientosReferencia.get(0);
        movimiento.setBilletera(billetera);
        movimiento.setConcepto(objetivo.compareTo(BigDecimal.ZERO) > 0
                ? "Productos canjeables por recompensa #" + recompensa.getId()
                : "Productos no disponibles por recompensa #" + recompensa.getId());
        movimiento.setMonto(objetivo);
        movimiento.setSaldoResultado(billetera.getSaldoProductos());
        movimiento.setPeriodo(periodoActivo);
        movimientoBilleteraDao.save(movimiento);
    }

    private List<Recompensa> recompensasCobrables(Long personaId) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .toList();
    }

    private List<Recompensa> recompensasMensualesCobrables(Long personaId) {
        return recompensasMensualesCobrables(personaId, null);
    }

    private List<Recompensa> recompensasMensualesCobrables(Long personaId, PeriodoGestion periodo) {
        return recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .filter(recompensa -> Boolean.TRUE.equals(recompensa.getCobrable()))
                .filter(recompensa -> periodo == null || recompensa.getPeriodo() != null && periodo.getId().equals(recompensa.getPeriodo().getId()))
                .filter(recompensa -> Optional.ofNullable(recompensa.getNivelGenerado()).orElse(0) >= 2)
                .toList();
    }

    private BigDecimal efectivoBilleteraDisponiblePeriodo(Long personaId, PeriodoGestion periodo) {
        return saldoPeriodo(personaId, periodo, MovimientoBilletera.TIPO_DINERO);
    }

    private BigDecimal saldoPeriodo(Long personaId, PeriodoGestion periodo, String tipo) {
        if (periodo == null || periodo.getId() == null) {
            return BigDecimal.ZERO;
        }
        return movimientoBilleteraDao.findByBilleteraPersonaIdAndPeriodoIdOrderByFechaRegistroDesc(personaId, periodo.getId()).stream()
                .filter(movimiento -> tipo.equals(movimiento.getTipo()))
                .map(MovimientoBilletera::getMonto)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO);
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

    private LocalDateTime finDeDia(LocalDate fecha) {
        return LocalDateTime.of(fecha, LocalTime.of(23, 59, 59));
    }
}
