package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionNivelDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CompraService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import com.vidayoung.platform.Model.Service.ReprocesoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
public class ReprocesoServiceImpl implements ReprocesoService {

    private static final int MAX_DETALLES = 200;

    private static final String REFERENCIA_COMPRA_RED = "COMPRA_RED";

    private final CompraDao compraDao;
    private final CompraService compraService;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    private final BilleteraDao billeteraDao;
    private final PersonaDao personaDao;
    private final PlanActivacionDao planActivacionDao;
    private final PlanActivacionNivelDao planActivacionNivelDao;
    private final ReferidoDao referidoDao;
    private final BilleteraService billeteraService;
    private final GestionPeriodoService gestionPeriodoService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReprocesoResumen simular() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        ReprocesoResumen resumen = ejecutar(false);
        return new ReprocesoResumen(
                true,
                resumen.beneficiariosRecalculados(),
                resumen.dineroTotalCreditado(),
                resumen.inactivosOmitidos(),
                resumen.posibleDebitoOmitidos(),
                resumen.fallos(),
                resumen.detalles()
        );
    }

    @Override
    public ReprocesoResumen reprocesar(boolean notificar) {
        return ejecutar(notificar);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecreacionResumen simularRecreacion() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        RecreacionResumen resumen = ejecutarRecreacion(false);
        return new RecreacionResumen(
                true,
                resumen.comprasProcesadas(),
                resumen.beneficiosGenerados(),
                resumen.volumenesRedCreditados(),
                resumen.saldosPvPropioActualizados(),
                resumen.fallos(),
                resumen.detalles()
        );
    }

    @Override
    public RecreacionResumen recrearRecompensas(boolean notificar) {
        return ejecutarRecreacion(notificar);
    }

    /**
     * Recrea las recompensas de TODAS las compras validadas/confirmadas con la
     * logica vigente, compra por compra:
     * 0. Reconstruye saldo_pv_propio desde los movimientos historicos (PRIMERO:
     *    los beneficios se pagan segun el plan que determina ese PV propio).
     * 1. Revierte y elimina los beneficios anteriores (auditoria conservada).
     * 2. Los regenera con 10 niveles, alcance efectivo plan+rango y montos actuales.
     * 3. Asegura el volumen de red (PV+QP a 9 niveles), idempotente.
     */
    private RecreacionResumen ejecutarRecreacion(boolean notificar) {
        // Paso 0: reconstruir PV propio ANTES de generar beneficios
        int saldosPvPropioActualizados = reconstruirSaldosPvPropio();

        List<Compra> compras = compraDao.findAllByOrderByFechaCompraDesc().stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .filter(compra -> Compra.ESTADO_COMPRA_VALIDADA.equals(compra.getEstadoCompra())
                        || Compra.ESTADO_COMPRA_CONFIRMADA.equals(compra.getEstadoCompra()))
                .toList();

        int comprasProcesadas = 0;
        int beneficiosGenerados = 0;
        int volumenesRedCreditados = 0;
        int fallos = 0;
        List<String> detalles = new ArrayList<>();

        for (Compra compra : compras) {
            try {
                int beneficiosAntes = contarBeneficiosActivos(compra.getId());
                int redAntes = contarVolumenesRedActivos(compra.getId());

                compraService.reiniciarRecompensasCompra(compra.getId(), notificar);

                int beneficiosDespues = contarBeneficiosActivos(compra.getId());
                int redDespues = contarVolumenesRedActivos(compra.getId());

                comprasProcesadas++;
                beneficiosGenerados += Math.max(0, beneficiosDespues - beneficiosAntes);
                volumenesRedCreditados += Math.max(0, redDespues - redAntes);
            } catch (RuntimeException exception) {
                fallos++;
                addDetalle(detalles, "Compra #" + compra.getId() + ": " + exception.getMessage());
            }
        }

        return new RecreacionResumen(
                false,
                comprasProcesadas,
                beneficiosGenerados,
                volumenesRedCreditados,
                saldosPvPropioActualizados,
                fallos,
                detalles
        );
    }

    /** Reconstruye saldo_pv_propio de todas las billeteras desde movimientos COMPRA/PV activos. */
    private int reconstruirSaldosPvPropio() {
        int actualizados = 0;
        for (Object[] fila : movimientoBilleteraDao.sumarPvPropioPorBilletera()) {
            Long billeteraId = ((Number) fila[0]).longValue();
            BigDecimal totalPvPropio = fila[1] == null ? BigDecimal.ZERO : new BigDecimal(fila[1].toString());
            Billetera billetera = billeteraDao.findById(billeteraId).orElse(null);
            if (billetera == null) {
                continue;
            }
            if (zeroIfNull(billetera.getSaldoPvPropio()).compareTo(totalPvPropio) != 0) {
                billetera.setSaldoPvPropio(totalPvPropio);
                billeteraDao.save(billetera);
                actualizados++;
            }
        }
        return actualizados;
    }

    private int contarBeneficiosActivos(Long compraId) {        return (int) beneficioActivacionCompraDao.findByCompraId(compraId).stream()
                .filter(beneficio -> Auditoria.ESTADO_ACTIVO.equals(beneficio.getEstado()))
                .count();
    }

    private int contarVolumenesRedActivos(Long compraId) {
        return movimientoBilleteraDao.findByReferenciaTipoAndReferenciaId(REFERENCIA_COMPRA_RED, compraId).stream()
                .filter(movimiento -> Auditoria.ESTADO_ACTIVO.equals(movimiento.getEstado()))
                .toList()
                .size();
    }

    private ReprocesoResumen ejecutar(boolean notificar) {
        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        List<Long> beneficiarioIds = beneficioActivacionCompraDao
                .findBeneficiarioIdsByPeriodoIdAndEstado(periodoActivo.getId(), Auditoria.ESTADO_ACTIVO);

        int beneficiariosRecalculados = 0;
        int inactivosOmitidos = 0;
        int posibleDebitoOmitidos = 0;
        int fallos = 0;
        BigDecimal dineroTotalCreditado = BigDecimal.ZERO;
        List<String> detalles = new ArrayList<>();

        for (Long beneficiarioId : beneficiarioIds) {
            Ajuste ajuste = analizarBeneficiario(beneficiarioId, periodoActivo);
            switch (ajuste.estado()) {
                case INACTIVO -> inactivosOmitidos++;
                case DEBITO -> {
                    posibleDebitoOmitidos++;
                    addDetalle(detalles, "Persona " + beneficiarioId + ": omitida (posible debito por membresia inactiva)");
                }
                case CREDITO -> {
                    try {
                        Persona persona = personaDao.findById(beneficiarioId).orElse(null);
                        if (persona == null) {
                            break;
                        }
                        billeteraService.recalcularBeneficiosActivacion(persona, notificar);
                        beneficiariosRecalculados++;
                        dineroTotalCreditado = dineroTotalCreditado.add(ajuste.credito());
                    } catch (RuntimeException exception) {
                        fallos++;
                        addDetalle(detalles, "Persona " + beneficiarioId + ": " + exception.getMessage());
                    }
                }
                default -> {
                }
            }
        }

        return new ReprocesoResumen(
                false,
                beneficiariosRecalculados,
                dineroTotalCreditado,
                inactivosOmitidos,
                posibleDebitoOmitidos,
                fallos,
                detalles
        );
    }

    private Ajuste analizarBeneficiario(Long beneficiarioId, PeriodoGestion periodoActivo) {
        Persona persona = personaDao.findById(beneficiarioId).orElse(null);
        if (persona == null || persona.getId() == null) {
            return Ajuste.omitido();
        }

        if (!membresiaActiva(persona, periodoActivo)) {
            return Ajuste.inactivo();
        }

        Billetera billetera = billeteraDao.findByPersonaId(persona.getId()).orElse(null);
        if (billetera == null) {
            return Ajuste.omitido();
        }

        Optional<PlanActivacion> planActivo = obtenerPlanActivacionPorPv(billetera.getSaldoPvPropio());
        int alcanceEfectivo = billeteraService.calcularAlcanceEfectivo(persona, planActivo.orElse(null));
        int maxNivelConfigurado = planActivo
                .flatMap(plan -> planActivacionNivelDao.findFirstByPlanActivacionIdOrderByNumeroNivelDesc(plan.getId()))
                .map(PlanActivacionNivel::getNumeroNivel)
                .orElse(0);
        List<BeneficioActivacionCompra> beneficios = beneficioActivacionCompraDao
                .findByBeneficiarioIdAndPeriodoId(persona.getId(), periodoActivo.getId()).stream()
                .filter(beneficio -> Auditoria.ESTADO_ACTIVO.equals(beneficio.getEstado()))
                .toList();
        if (beneficios.isEmpty()) {
            return Ajuste.omitido();
        }

        BigDecimal totalCredito = BigDecimal.ZERO;
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
            boolean pagaNuevo = planActivo.isPresent() && nivelAplica && nuevoMontoTotal.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal montoAnterior = zeroIfNull(beneficio.getMontoTotal());
            BigDecimal diferencia = pagaNuevo
                    ? nuevoMontoTotal.subtract(montoAnterior)
                    : montoAnterior.negate();

            if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                return Ajuste.debito();
            }
            totalCredito = totalCredito.add(diferencia);
        }

        if (totalCredito.compareTo(BigDecimal.ZERO) <= 0) {
            return Ajuste.omitido();
        }
        return Ajuste.credito(totalCredito);
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

    private Optional<PlanActivacion> obtenerPlanActivacionPorPv(BigDecimal pvActual) {
        return planActivacionDao.findByPvMinimoMensualLessThanEqualOrderByPvMinimoMensualDesc(zeroIfNull(pvActual)).stream()
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .findFirst();
    }

    private void addDetalle(List<String> detalles, String detalle) {
        if (detalles.size() < MAX_DETALLES) {
            detalles.add(detalle);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final String INACTIVO = "INACTIVO";
    private static final String DEBITO = "DEBITO";
    private static final String CREDITO = "CREDITO";
    private static final String OMITIDO = "OMITIDO";

    private record Ajuste(String estado, BigDecimal credito) {

        static Ajuste inactivo() {
            return new Ajuste(INACTIVO, BigDecimal.ZERO);
        }

        static Ajuste debito() {
            return new Ajuste(DEBITO, BigDecimal.ZERO);
        }

        static Ajuste credito(BigDecimal credito) {
            return new Ajuste(CREDITO, credito);
        }

        static Ajuste omitido() {
            return new Ajuste(OMITIDO, BigDecimal.ZERO);
        }
    }
}