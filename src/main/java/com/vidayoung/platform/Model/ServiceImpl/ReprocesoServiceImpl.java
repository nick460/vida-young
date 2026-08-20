package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionNivelDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.Compra;
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

    private final CompraDao compraDao;
    private final BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    private final BilleteraDao billeteraDao;
    private final PersonaDao personaDao;
    private final PlanActivacionDao planActivacionDao;
    private final PlanActivacionNivelDao planActivacionNivelDao;
    private final ReferidoDao referidoDao;
    private final CompraService compraService;
    private final BilleteraService billeteraService;
    private final GestionPeriodoService gestionPeriodoService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReprocesoResumen simular() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        ReprocesoResumen resumen = ejecutar(false);
        return new ReprocesoResumen(
                true,
                resumen.comprasProcesadas(),
                resumen.bonosQpCreditados(),
                resumen.qpTotalCreditado(),
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

    private ReprocesoResumen ejecutar(boolean notificar) {
        List<Compra> compras = compraDao.findAllByOrderByFechaCompraDesc().stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .filter(compra -> Compra.ESTADO_COMPRA_VALIDADA.equals(compra.getEstadoCompra()))
                .filter(compra -> zeroIfNull(compra.getTotalQpBonoReferido()).compareTo(BigDecimal.ZERO) > 0)
                .toList();

        int comprasProcesadas = 0;
        int bonosQpCreditados = 0;
        int fallos = 0;
        BigDecimal qpTotalCreditado = BigDecimal.ZERO;
        List<String> detalles = new ArrayList<>();

        for (Compra compra : compras) {
            comprasProcesadas++;
            try {
                int creditados = compraService.reprocesarQpBonoReferido(compra.getId(), notificar);
                bonosQpCreditados += creditados;
                if (creditados > 0) {
                    qpTotalCreditado = qpTotalCreditado.add(
                            zeroIfNull(compra.getTotalQpBonoReferido()).multiply(BigDecimal.valueOf(creditados)));
                }
            } catch (RuntimeException exception) {
                fallos++;
                addDetalle(detalles, "Compra #" + compra.getId() + ": " + exception.getMessage());
            }
        }

        PeriodoGestion periodoActivo = gestionPeriodoService.obtenerPeriodoActivo();
        List<Long> beneficiarioIds = beneficioActivacionCompraDao
                .findBeneficiarioIdsByPeriodoIdAndEstado(periodoActivo.getId(), Auditoria.ESTADO_ACTIVO);

        int beneficiariosRecalculados = 0;
        int inactivosOmitidos = 0;
        int posibleDebitoOmitidos = 0;
        BigDecimal dineroTotalCreditado = BigDecimal.ZERO;

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
                comprasProcesadas,
                bonosQpCreditados,
                qpTotalCreditado,
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

        Optional<PlanActivacion> planActivo = obtenerPlanActivacionPorPv(billetera.getSaldoPv());
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
            PlanActivacionNivel nivelConfig = planActivo
                    .map(PlanActivacion::getId)
                    .flatMap(planId -> planActivacionNivelDao.findByPlanActivacionIdAndNumeroNivel(planId, nivel))
                    .orElse(null);
            BigDecimal nuevoMontoPorProducto = nivelConfig == null
                    ? BigDecimal.ZERO
                    : zeroIfNull(nivelConfig.getMontoPorProducto());
            BigDecimal nuevoMontoTotal = nuevoMontoPorProducto
                    .multiply(BigDecimal.valueOf(beneficio.getCantidadProductos()));
            boolean pagaNuevo = planActivo.isPresent() && nuevoMontoTotal.compareTo(BigDecimal.ZERO) > 0;
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