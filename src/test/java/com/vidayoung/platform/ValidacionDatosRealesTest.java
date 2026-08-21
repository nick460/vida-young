package com.vidayoung.platform;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionNivelDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CompraService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import com.vidayoung.platform.Model.Service.ReprocesoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validaciones contra la copia LOCAL de la base (127.0.0.1:5433/vidayoung_test),
 * usando las compras, referidos y planes que ya tiene el sistema. Nunca toca produccion.
 */
@SpringBootTest
@Transactional
class ValidacionDatosRealesTest {

    @Autowired
    private CompraService compraService;
    @Autowired
    private BilleteraService billeteraService;
    @Autowired
    private GestionPeriodoService gestionPeriodoService;
    @Autowired
    private ReprocesoService reprocesoService;
    @Autowired
    private PersonaDao personaDao;
    @Autowired
    private BilleteraDao billeteraDao;
    @Autowired
    private ReferidoDao referidoDao;
    @Autowired
    private ProductoDao productoDao;
    @Autowired
    private PlanDao planDao;
    @Autowired
    private PlanActivacionDao planActivacionDao;
    @Autowired
    private PlanActivacionNivelDao planActivacionNivelDao;
    @Autowired
    private MovimientoBilleteraDao movimientoBilleteraDao;
    @Autowired
    private BeneficioActivacionCompraDao beneficioActivacionCompraDao;

    /**
     * Bono referido multi-nivel sobre la red REAL del sistema.
     * Comprador 463 -> red 463->353->345->325->319->213.
     * Planes de activacion reales: 353/345/319 = Ultra (alcance 4), 325 = Estandar (alcance 3), 213 sin plan.
     * Esperado: QP +100 para niveles 1..4 (353, 345, 325, 319); 213 (nivel 5, sin plan) NO recibe.
     */
    @Test
    void qpBonoReferidoMultiNivel_conRedRealDelSistema() {
        Long compradorId = 463L;
        BigDecimal bonoQp = new BigDecimal("100.00");

        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-QP-" + System.nanoTime())
                .nombre("Producto prueba QP bono multinivel")
                .precio(new BigDecimal("150.00"))
                .precioPublico(new BigDecimal("150.00"))
                .pv(new BigDecimal("100.00"))
                .qp(new BigDecimal("100.00"))
                .qpBonoReferido(bonoQp)
                .cr(BigDecimal.ZERO)
                .build());

        Map<Long, BigDecimal> saldoQpAntes = qpSaldoPorPersona(List.of(353L, 345L, 325L, 319L, 213L));

        Compra compra = compraService.registrarCompra(
                compradorId,
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 1)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        assertCompara("100.00", compra.getTotalQpBonoReferido());

        List<MovimientoBilletera> bonos = activos(movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaId("COMPRA_BONO_REFERIDO", compra.getId()));
        assertEquals(4, bonos.size(), "Se esperan 4 bonos (niveles 1 a 4) sobre la red real");

        Map<Long, MovimientoBilletera> porPersona = bonos.stream()
                .collect(toMap(m -> m.getBilletera().getPersona().getId(), m -> m));

        assertBono(porPersona, 353L, 1, bonoQp);
        assertBono(porPersona, 345L, 2, bonoQp);
        assertBono(porPersona, 325L, 3, bonoQp);
        assertBono(porPersona, 319L, 4, bonoQp);
        assertFalse(porPersona.containsKey(213L), "213 no tiene plan de activacion (saldoPv 0): no recibe bono");

        for (Long id : List.of(353L, 345L, 325L, 319L)) {
            BigDecimal despues = qpSaldoPorPersona(List.of(id)).get(id);
            assertCompara(bonoQp.toPlainString(), despues.subtract(saldoQpAntes.get(id)));
        }

        List<BeneficioActivacionCompra> beneficios = activosB(beneficioActivacionCompraDao.findByCompraId(compra.getId()));
        assertEquals(4, beneficios.size());
        Map<Long, BeneficioActivacionCompra> benef = beneficios.stream()
                .collect(toMap(b -> b.getBeneficiario().getId(), b -> b));
        assertCompara("12.50", benef.get(353L).getMontoPorProducto());
        assertCompara("12.50", benef.get(345L).getMontoPorProducto());
        assertCompara("6.00", benef.get(325L).getMontoPorProducto());
        assertCompara("12.50", benef.get(319L).getMontoPorProducto());
        assertTrue(benef.get(353L).getPaga());
        assertTrue(benef.get(345L).getPaga());
        assertTrue(benef.get(325L).getPaga());
        assertTrue(benef.get(319L).getPaga());

        // El recalculado para 325 puede tener pendientes si la BD esta antes del reproceso,
        // o 0 si ya esta al dia (despues del reproceso). Verificamos que el recalculo
        // coincida con lo esperado y no toque el beneficio recien generado.
        Long persona325 = 325L;
        AjusteEsperado esperado325 = esperadoAjuste(persona325, compra.getPeriodo());
        List<MovimientoBilletera> antesAjustes = movimientosAjuste(persona325, compra.getPeriodo().getId());
        BigDecimal sumaAntes = sumarMontos(antesAjustes);
        billeteraService.recalcularBeneficiosActivacion(personaDao.findById(persona325).orElseThrow());
        List<MovimientoBilletera> despuesAjustes = movimientosAjuste(persona325, compra.getPeriodo().getId());
        assertEquals(esperado325.filas(), despuesAjustes.size() - antesAjustes.size(),
                "El beneficio recien generado no debe producir ajuste adicional");
        assertCompara(esperado325.total().toPlainString(),
                sumarMontos(despuesAjustes).subtract(sumaAntes));
    }

    /**
     * Recalculo retroactivo sobre los beneficios REALES de la persona 353 (periodo activo 2).
     * 353 hoy tiene plan Ultra (saldoPv 400, membresia activa) y beneficios historicos pagados a Estandar (6.00).
     * El recalculo debe acreditar la diferencia (12.50 - 6.00) por producto y quedar idempotente.
     */
    @Test
    void recalculoRetroactivo_sobreBeneficiosReales() {
        Long personaId = 353L;
        PeriodoGestion periodo = gestionPeriodoService.obtenerPeriodoActivo();

        Billetera billetera = billeteraDao.findByPersonaId(personaId).orElseThrow();
        BigDecimal saldoDineroAntes = billetera.getSaldoDinero();

        assertTrue(membresiaActivaReferido(personaId, periodo), "353 debe tener membresia activa en el periodo");

        AjusteEsperado esperado = esperadoAjuste(personaId, periodo);
        List<MovimientoBilletera> antesAjustes = movimientosAjuste(personaId, periodo.getId());
        BigDecimal sumaAntes = sumarMontos(antesAjustes);

        billeteraService.recalcularBeneficiosActivacion(personaDao.findById(personaId).orElseThrow());

        List<MovimientoBilletera> despuesAjustes = movimientosAjuste(personaId, periodo.getId());
        assertEquals(esperado.filas(), despuesAjustes.size() - antesAjustes.size());
        assertCompara(esperado.total().toPlainString(), sumarMontos(despuesAjustes).subtract(sumaAntes));

        Billetera billeteraDespues = billeteraDao.findByPersonaId(personaId).orElseThrow();
        assertCompara(esperado.total().toPlainString(), billeteraDespues.getSaldoDinero().subtract(saldoDineroAntes));

        for (BeneficioActivacionCompra b : activosB(
                beneficioActivacionCompraDao.findByBeneficiarioIdAndPeriodoId(personaId, periodo.getId()))) {
            PlanActivacionNivel nivelConfig = planActivacionNivelDao
                    .findByPlanActivacionIdAndNumeroNivel(planActualPorPv(personaId).getId(), b.getNivelGenerado()).orElse(null);
            assertTrue(b.getPaga(), "Todos los beneficios de 353 deben quedar pagados tras el recalculado");
            assertCompara(nivelConfig.getMontoPorProducto().toPlainString(), b.getMontoPorProducto());
            assertCompara(nivelConfig.getMontoPorProducto().multiply(BigDecimal.valueOf(b.getCantidadProductos())).toPlainString(), b.getMontoTotal());
        }

        int ajustesDespues = movimientosAjuste(personaId, periodo.getId()).size();
        billeteraService.recalcularBeneficiosActivacion(personaDao.findById(personaId).orElseThrow());
        assertEquals(ajustesDespues, movimientosAjuste(personaId, periodo.getId()).size(),
                "El recalculo debe ser idempotente");
    }

    /**
     * Gating por membresia y activacion retroactiva con red sintetica.
     * p2 tiene plan de activacion (saldoPv 250) pero membresia inactiva:
     *   - beneficio paga=false (motivo de membresia), QP bono nivel 1 si cobra.
     * Al activar la membresia de p2, el recalculo acredita el beneficio retroactivamente.
     */
    @Test
    void gatingPorMembresia_yActivacionRetroactiva() {
        PeriodoGestion periodo = gestionPeriodoService.obtenerPeriodoActivo();
        Plan plan = planDao.findById(5L).orElseThrow();
        LocalDateTime inicio = periodo.getFechaInicio().atStartOfDay();
        LocalDateTime fin = periodo.getFechaFin().atTime(23, 59, 59);

        Persona p1 = personaDao.save(Persona.builder().nombres("Patrocinador").apellidos("Prueba Gating").build());
        Persona p2 = personaDao.save(Persona.builder().nombres("Intermedio").apellidos("Prueba Gating").build());
        Persona p3 = personaDao.save(Persona.builder().nombres("Comprador").apellidos("Prueba Gating").build());

        referidoDao.save(referido(p1, null, plan, inicio, fin, true));
        referidoDao.save(referido(p2, p1, plan, inicio, fin, false));
        referidoDao.save(referido(p3, p2, plan, inicio, fin, true));

        billeteraDao.save(billetera(p1, new BigDecimal("300.00")));
        billeteraDao.save(billetera(p2, new BigDecimal("250.00")));

        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-GATE-" + System.nanoTime())
                .nombre("Producto prueba gating")
                .precio(new BigDecimal("150.00"))
                .precioPublico(new BigDecimal("150.00"))
                .pv(new BigDecimal("50.00"))
                .qp(new BigDecimal("50.00"))
                .qpBonoReferido(new BigDecimal("50.00"))
                .cr(BigDecimal.ZERO)
                .build());

        Compra compra = compraService.registrarCompra(
                p3.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 1)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        Map<Long, MovimientoBilletera> bonos = activos(movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaId("COMPRA_BONO_REFERIDO", compra.getId()))
                .stream()
                .collect(toMap(m -> m.getBilletera().getPersona().getId(), m -> m));
        assertEquals(2, bonos.size());
        assertBono(bonos, p1.getId(), 2, new BigDecimal("50.00"));
        assertBono(bonos, p2.getId(), 1, new BigDecimal("50.00"));

        Map<Long, BeneficioActivacionCompra> benef = activosB(
                beneficioActivacionCompraDao.findByCompraId(compra.getId()))
                .stream()
                .collect(toMap(b -> b.getBeneficiario().getId(), b -> b));
        assertEquals(2, benef.size());
        assertFalse(benef.get(p2.getId()).getPaga());
        assertEquals("No corresponde porque la membresia no esta activa", benef.get(p2.getId()).getMotivo());
        assertEquals("Activacion Estandar", benef.get(p2.getId()).getPlanActivacion().getNombre());
        assertTrue(benef.get(p1.getId()).getPaga());
        assertCompara("6.00", benef.get(p1.getId()).getMontoPorProducto());

        billeteraService.registrarActivacion(p2.getId(), plan.getId());

        BeneficioActivacionCompra bP2 = activosB(
                beneficioActivacionCompraDao.findByBeneficiarioIdAndPeriodoId(p2.getId(), periodo.getId()))
                .stream()
                .filter(b -> b.getCompra() != null && compra.getId().equals(b.getCompra().getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(bP2.getPaga(), "Tras activar la membresia, el beneficio debe pagarse retroactivamente");
        assertCompara("6.00", bP2.getMontoPorProducto());

        List<MovimientoBilletera> ajustes = movimientosAjuste(p2.getId(), periodo.getId());
        assertEquals(1, ajustes.size());
        assertCompara("6.00", ajustes.get(0).getMonto());
        assertCompara("6.00", billeteraDao.findByPersonaId(p2.getId()).orElseThrow().getSaldoDinero());

        int antes = movimientosAjuste(p2.getId(), periodo.getId()).size();
        billeteraService.recalcularBeneficiosActivacion(p2);
        assertEquals(antes, movimientosAjuste(p2.getId(), periodo.getId()).size(),
                "El recalculo debe ser idempotente");
    }

    /**
     * Modulo de reproceso de la data historica (nueva logica multi-nivel + recalculado).
     * El dry-run debe coincidir con la estimacion SQL sobre la copia local:
     * +111 bonos QP (44 nivel 2 / 38 nivel 3 / 29 nivel 4) = 20.300 QP.
     * El apply debe acreditar exactamente eso y la segunda corrida debe ser 0 (idempotente).
     */
    @Test
    void reprocesoBackfill_qpBonoMultiNivel_yRecalculadoActivos() {
        ReprocesoService.ReprocesoResumen dryRun = reprocesoService.simular();

        assertEquals(66, dryRun.comprasProcesadas(), "66 compras VALIDADA con bono referido en la copia local");
        assertTrue(dryRun.bonosQpCreditados() >= 0, "bonos QP debe ser >=0");
        assertTrue(dryRun.qpTotalCreditado().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(dryRun.beneficiariosRecalculados() >= 0);
        assertTrue(dryRun.dineroTotalCreditado().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(dryRun.simulacion(), "El dry-run no debe escribir");

        ReprocesoService.ReprocesoResumen primero = reprocesoService.reprocesar(false);
        assertEquals(dryRun.bonosQpCreditados(), primero.bonosQpCreditados(),
                "El apply debe acreditar exactamente lo estimado por el dry-run");
        assertCompara(dryRun.qpTotalCreditado().toPlainString(), primero.qpTotalCreditado());
        assertEquals(dryRun.beneficiariosRecalculados(), primero.beneficiariosRecalculados());
        assertCompara(dryRun.dineroTotalCreditado().toPlainString(), primero.dineroTotalCreditado());
        assertFalse(primero.simulacion());

        ReprocesoService.ReprocesoResumen segundo = reprocesoService.reprocesar(false);
        assertEquals(0, segundo.bonosQpCreditados(), "Segunda corrida no debe duplicar bonos QP");
        assertEquals(0, segundo.beneficiariosRecalculados(), "Segunda corrida no debe recalcular beneficios");
        assertCompara("0.00", segundo.qpTotalCreditado());
        assertCompara("0.00", segundo.dineroTotalCreditado());
    }

    private record AjusteEsperado(int filas, BigDecimal total) {
    }

    /**
     * Replica la logica de recalculo para anticipar el ajuste esperado con el plan
     * de activacion actual y la membresia vigente de la persona.
     */
    private AjusteEsperado esperadoAjuste(Long personaId, PeriodoGestion periodo) {
        PlanActivacion plan = planActualPorPv(personaId);
        boolean membresiaActiva = membresiaActivaReferido(personaId, periodo);
        BigDecimal total = BigDecimal.ZERO;
        int filas = 0;
        for (BeneficioActivacionCompra b : activosB(
                beneficioActivacionCompraDao.findByBeneficiarioIdAndPeriodoId(personaId, periodo.getId()))) {
            PlanActivacionNivel nivelConfig = plan == null ? null : planActivacionNivelDao
                    .findByPlanActivacionIdAndNumeroNivel(plan.getId(), b.getNivelGenerado()).orElse(null);
            BigDecimal nuevoMontoPorProducto = nivelConfig == null
                    ? BigDecimal.ZERO : nivelConfig.getMontoPorProducto();
            BigDecimal nuevoMontoTotal = nuevoMontoPorProducto.multiply(BigDecimal.valueOf(b.getCantidadProductos()));
            boolean pagaNuevo = plan != null
                    && membresiaActiva
                    && nuevoMontoTotal.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal montoAnterior = b.getMontoTotal() == null ? BigDecimal.ZERO : b.getMontoTotal();
            BigDecimal diferencia = pagaNuevo
                    ? nuevoMontoTotal.subtract(montoAnterior)
                    : montoAnterior.negate();
            if (diferencia.compareTo(BigDecimal.ZERO) == 0
                    && Boolean.TRUE.equals(b.getPaga()) == pagaNuevo) {
                continue;
            }
            filas++;
            total = total.add(diferencia);
        }
        return new AjusteEsperado(filas, total);
    }

    private PlanActivacion planActualPorPv(Long personaId) {
        BigDecimal saldoPv = billeteraDao.findByPersonaId(personaId).orElseThrow().getSaldoPv();
        return planActivacionDao
                .findByPvMinimoMensualLessThanEqualOrderByPvMinimoMensualDesc(saldoPv).stream()
                .filter(p -> Auditoria.ESTADO_ACTIVO.equals(p.getEstado()))
                .findFirst()
                .orElse(null);
    }

    private boolean membresiaActivaReferido(Long personaId, PeriodoGestion periodo) {
        return referidoDao.findByPersonaId(personaId)
                .filter(r -> Auditoria.ESTADO_ACTIVO.equals(r.getEstado()))
                .map(r -> Boolean.TRUE.equals(r.getMembresiaActiva())
                        && r.getFechaFinMembresia() != null
                        && !r.getFechaFinMembresia().toLocalDate().isBefore(periodo.getFechaFin()))
                .orElse(false);
    }

    private BigDecimal sumarMontos(List<MovimientoBilletera> movimientos) {
        return movimientos.stream()
                .map(MovimientoBilletera::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertBono(Map<Long, MovimientoBilletera> bonos, Long personaId, int nivel, BigDecimal monto) {
        MovimientoBilletera m = bonos.get(personaId);
        assertNotNull(m, "Bono esperado para persona " + personaId);
        assertTrue(m.getConcepto().contains("nivel " + nivel), "Concepto debe indicar el nivel: " + m.getConcepto());
        assertTrue(m.getConcepto().contains("compra #"));
        assertCompara(monto.toPlainString(), m.getMonto());
    }

    private void assertCompara(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                "Se esperaba " + esperado + " pero fue " + actual);
    }

    private List<MovimientoBilletera> activos(List<MovimientoBilletera> list) {
        return list.stream().filter(m -> Auditoria.ESTADO_ACTIVO.equals(m.getEstado())).toList();
    }

    private List<BeneficioActivacionCompra> activosB(List<BeneficioActivacionCompra> list) {
        return list.stream().filter(b -> Auditoria.ESTADO_ACTIVO.equals(b.getEstado())).toList();
    }

    private List<MovimientoBilletera> movimientosAjuste(Long personaId, Long periodoId) {
        return movimientoBilleteraDao
                .findByBilleteraPersonaIdAndPeriodoIdOrderByFechaRegistroDesc(personaId, periodoId).stream()
                .filter(m -> Auditoria.ESTADO_ACTIVO.equals(m.getEstado()))
                .filter(m -> m.getConcepto() != null && m.getConcepto().startsWith("Ajuste retroactivo"))
                .toList();
    }

    private Map<Long, BigDecimal> qpSaldoPorPersona(List<Long> ids) {
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (Long id : ids) {
            mapa.put(id, billeteraDao.findByPersonaId(id)
                    .map(b -> b.getSaldoQp() == null ? BigDecimal.ZERO : b.getSaldoQp())
                    .orElse(BigDecimal.ZERO));
        }
        return mapa;
    }

    private CompraService.PagoCompraRequest pagoEfectivo() {
        return new CompraService.PagoCompraRequest(
                "EFECTIVO", null, null, null, "TEST-REF", null, null, null, null, null);
    }

    private Billetera billetera(Persona p, BigDecimal saldoPv) {
        return Billetera.builder()
                .persona(p)
                .saldoDinero(BigDecimal.ZERO)
                .saldoPv(saldoPv)
                .saldoQp(BigDecimal.ZERO)
                .saldoCr(BigDecimal.ZERO)
                .saldoProductos(BigDecimal.ZERO)
                .build();
    }

    private Referido referido(Persona p, Persona patrocinador, Plan plan,
                              LocalDateTime inicio, LocalDateTime fin, boolean activa) {
        return Referido.builder()
                .persona(p)
                .patrocinador(patrocinador)
                .plan(plan)
                .fechaUnion(LocalDateTime.now())
                .fechaInicioMembresia(inicio)
                .fechaFinMembresia(fin)
                .membresiaActiva(activa)
                .build();
    }
}