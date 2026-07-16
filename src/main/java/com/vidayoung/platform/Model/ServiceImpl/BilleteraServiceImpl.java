package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.HistorialMembresiaDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.RangoDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
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
    private final RangoDao rangoDao;
    private final RecompensaDao recompensaDao;
    private final ReferidoDao referidoDao;
    private final CarteraEmpresaService carteraEmpresaService;

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
                        .build()));
    }

    @Override
    public Optional<Billetera> buscarPorPersonaId(Long personaId) {
        return billeteraDao.findByPersonaId(personaId);
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
                    .build());
        }

        return historial;
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
        String periodo = YearMonth.now().toString();
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
            Rango rango = rangoAlcanzadoPorQp(saldoQp).orElse(null);

            CierreMensualBilletera cierre = cierreMensualBilleteraDao.save(CierreMensualBilletera.builder()
                    .persona(billetera.getPersona())
                    .periodo(periodo)
                    .saldoDinero(saldoDinero)
                    .saldoPv(saldoPv)
                    .saldoQp(saldoQp)
                    .saldoCr(saldoCr)
                    .rango(rango)
                    .rangoNombre(rango == null ? null : rango.getNombre())
                    .rangoQpMinimo(rango == null ? null : zeroIfNull(rango.getQpMinimo()))
                    .estadoPlanilla(CierreMensualBilletera.ESTADO_PLANILLA_PENDIENTE)
                    .fechaCierre(fechaCierre)
                    .build());

            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_DINERO, saldoDinero);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_PV, saldoPv);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_QP, saldoQp);
            registrarMovimientoCierreSiAplica(billetera, cierre, MovimientoBilletera.TIPO_CR, saldoCr);

            billetera.setSaldoDinero(BigDecimal.ZERO);
            billetera.setSaldoPv(BigDecimal.ZERO);
            billetera.setSaldoQp(BigDecimal.ZERO);
            billetera.setSaldoCr(BigDecimal.ZERO);
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
                    recompensaDao.save(recompensa);
                });
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
            BigDecimal saldo
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
                .build());
    }

    private LocalDateTime calcularFechaFinMembresia(LocalDateTime fechaInicio) {
        LocalDate fechaFin = fechaInicio.toLocalDate().plusMonths(1);
        return LocalDateTime.of(fechaFin, LocalTime.of(23, 59, 59));
    }
}
