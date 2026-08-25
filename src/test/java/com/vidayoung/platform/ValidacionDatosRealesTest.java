package com.vidayoung.platform;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.RangoDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.CompraService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import com.vidayoung.platform.Model.Service.ReprocesoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validaciones de las REGLAS NUEVAS contra la BD local de pruebas
 * (PostgreSQL 16 en localhost:5434/vidayoung_test). Nunca toca produccion.
 *
 * Reglas validadas:
 * 1. Al validar una compra, el PV y QP del comprador sube por la red hasta 9 niveles
 *    (comprador + 9 = 10), sin condiciones de plan ni membresia (referencia COMPRA_RED).
 * 2. El CR queda solo para el comprador.
 * 3. La membresia se activa unicamente con el PV de compras propias (saldo_pv_propio);
 *    el PV recibido de la red suma al saldo pero NO activa membresia.
 */
@SpringBootTest
@Transactional
class ValidacionDatosRealesTest {

    private static final String REF_RED = "COMPRA_RED";

    @Autowired
    private CompraService compraService;
    @Autowired
    private BilleteraService billeteraService;
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
    private MovimientoBilleteraDao movimientoBilleteraDao;
    @Autowired
    private BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    @Autowired
    private RangoDao rangoDao;
    @Autowired
    private GestionPeriodoService gestionPeriodoService;

    /**
     * El PV y QP de una compra validada sube por la red SIN condiciones:
     * cada upline dentro de los 9 niveles recibe el monto completo por COMPRA_RED,
     * tenga o no plan o membresia activa. El CR queda solo en el comprador.
     */
    @Test
    void volumenRed_subenPvYQpSinCondicionesYCrSeQuedaEnElComprador() {
        // Cadena sintetica: p1 <- p2 <- p3(comprador)
        Persona p1 = personaDao.save(Persona.builder().nombres("Red").apellidos("Abuela").build());
        Persona p2 = personaDao.save(Persona.builder().nombres("Red").apellidos("Madre").build());
        Persona p3 = personaDao.save(Persona.builder().nombres("Red").apellidos("Comprador").build());

        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();
        referidoDao.save(referido(p3, p2, plan, ahora, ahora.plusDays(30), false));
        referidoDao.save(referido(p2, p1, plan, ahora, ahora.plusDays(30), false));
        // p1 SIN membresia activa y sin billetera previa: bajo las reglas viejas en nivel 2
        // no cobraria; con las nuevas debe recibir igual.

        BigDecimal pvCompra = new BigDecimal("40.00");
        BigDecimal qpCompra = new BigDecimal("20.00");
        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-RED-" + System.nanoTime())
                .nombre("Producto prueba volumen red")
                .precio(new BigDecimal("120.00"))
                .precioPublico(new BigDecimal("120.00"))
                .pv(pvCompra)
                .qp(qpCompra)
                .cr(new BigDecimal("7.00"))
                .build());

        Compra compra = compraService.registrarCompra(
                p3.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 1)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        // Comprador: volumenes propios por COMPRA
        List<MovimientoBilletera> propios = activos(movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaId("COMPRA", compra.getId()));
        Map<String, MovimientoBilletera> propiosComprador = propios.stream()
                .filter(m -> m.getBilletera().getPersona().getId().equals(p3.getId()))
                .collect(toMap(m -> m.getTipo(), m -> m, (a, b) -> a));
        assertTrue(propiosComprador.containsKey(MovimientoBilletera.TIPO_PV));
        assertTrue(propiosComprador.containsKey(MovimientoBilletera.TIPO_QP));
        assertTrue(propiosComprador.containsKey(MovimientoBilletera.TIPO_CR));
        assertCompara(pvCompra.toPlainString(), propiosComprador.get(MovimientoBilletera.TIPO_PV).getMonto());

        // Uplines p2 (nivel 1) y p1 (nivel 2): reciben PV y QP completos por COMPRA_RED
        for (Persona upline : List.of(p2, p1)) {
            List<MovimientoBilletera> red = activos(movimientoBilleteraDao
                    .findByReferenciaTipoAndReferenciaIdAndBilleteraPersonaId(REF_RED, compra.getId(), upline.getId()));
            assertEquals(2, red.size(), upline.getApellidos() + " debe tener PV y QP por COMPRA_RED");
            Map<String, MovimientoBilletera> porTipo = red.stream()
                    .collect(toMap(MovimientoBilletera::getTipo, m -> m, (a, b) -> a));
            assertCompara(pvCompra.toPlainString(), porTipo.get(MovimientoBilletera.TIPO_PV).getMonto());
            assertCompara(qpCompra.toPlainString(), porTipo.get(MovimientoBilletera.TIPO_QP).getMonto());
            assertNull(porTipo.get(MovimientoBilletera.TIPO_CR),
                    "El CR no sube por la red");
            assertTrue(porTipo.get(MovimientoBilletera.TIPO_PV).getConcepto().contains("nivel "));
        }
    }

    /**
     * La membresia se activa SOLO con PV propio:
     * - El comprador cruza el umbral del plan con su propia compra -> se activa.
     * - El upline recibe mas PV que el umbral desde la red, pero su saldoPvPropio
     *   sigue en 0 -> NO se activa.
     */
    @Test
    void membresia_seActivaSoloConPvPropio_noConPvDeRed() {
        PlanActivacion planMinimo = planActivacionDao.findAll().stream()
                .filter(p -> Auditoria.ESTADO_ACTIVO.equals(p.getEstado()))
                .min(java.util.Comparator.comparing(PlanActivacion::getPvMinimoMensual))
                .orElseThrow(() -> new IllegalStateException("No hay planes de activacion en la BD"));
        BigDecimal umbral = planMinimo.getPvMinimoMensual() == null
                ? BigDecimal.ZERO : planMinimo.getPvMinimoMensual();

        // El producto da suficiente PV propio para cruzar el umbral minimo
        BigDecimal pvProducto = umbral.add(new BigDecimal("10.00"));
        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-PVPROPIO-" + System.nanoTime())
                .nombre("Producto prueba pv propio")
                .precio(pvProducto.multiply(BigDecimal.TEN))
                .precioPublico(pvProducto.multiply(BigDecimal.TEN))
                .pv(pvProducto)
                .qp(BigDecimal.ONE)
                .cr(BigDecimal.ZERO)
                .build());

        Persona p1 = personaDao.save(Persona.builder().nombres("Pv").apellidos("Upline").build());
        Persona p2 = personaDao.save(Persona.builder().nombres("Pv").apellidos("Comprador").build());
        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();
        referidoDao.save(referido(p2, p1, plan, ahora, ahora.minusDays(1), false));
        referidoDao.save(referido(p1, null, plan, ahora.minusDays(30), ahora.minusDays(1), false));

        // Upline ya supera el umbral por saldo total, pero TODO es PV de red (propio = 0)
        billeteraDao.save(billetera(p1, umbral.add(new BigDecimal("500.00")), BigDecimal.ZERO));
        // Comprador arranca sin volumenes
        billeteraDao.save(billetera(p2, BigDecimal.ZERO, BigDecimal.ZERO));

        Compra compra = compraService.registrarCompra(
                p2.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 1)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        // Comprador: su PV propio cruzo el umbral -> membresia activada hasta fin de periodo vigente
        Referido r2 = referidoDao.findByPersonaId(p2.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(r2.getMembresiaActiva()),
                "El comprador debe activar membresia por PV propio");
        assertNotNull(r2.getFechaInicioMembresia());

        // Upline: recibio PV de red (saldo total crece) pero NO activa membresia
        Billetera b1 = billeteraDao.findByPersonaId(p1.getId()).orElseThrow();
        assertTrue(b1.getSaldoPv().compareTo(umbral) > 0, "El upline recibio el PV de red");
        assertCompara("0.00", b1.getSaldoPvPropio());
        Referido r1 = referidoDao.findByPersonaId(p1.getId()).orElseThrow();
        assertFalse(Boolean.TRUE.equals(r1.getMembresiaActiva()),
                "El PV de la red NO debe activar la membresia del upline");
        assertTrue(r1.getFechaFinMembresia().toLocalDate().isBefore(java.time.LocalDate.now()),
                "La membresia del upline sigue expirada");

        // El comprador acumulo su propio PV
        Billetera b2 = billeteraDao.findByPersonaId(p2.getId()).orElseThrow();
        assertCompara(pvProducto.add(new BigDecimal("0")).toPlainString(), b2.getSaldoPvPropio());
    }

    /**
     * El reproceso ya no acredita bonos QP (esa fase fue eliminada):
     * solo recalcula beneficios de activacion. Dry-run no escribe, apply es idempotente.
     */
    @Test
    void reproceso_soloRecalculaBeneficios_yEsIdempotente() {
        ReprocesoService.ReprocesoResumen dryRun = reprocesoService.simular();

        assertTrue(dryRun.simulacion(), "El dry-run no debe escribir");
        assertTrue(dryRun.beneficiariosRecalculados() >= 0);
        assertTrue(dryRun.dineroTotalCreditado().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(dryRun.fallos() >= 0);

        ReprocesoService.ReprocesoResumen primero = reprocesoService.reprocesar(false);
        assertFalse(primero.simulacion());

        ReprocesoService.ReprocesoResumen segundo = reprocesoService.reprocesar(false);
        assertEquals(0, segundo.beneficiariosRecalculados(),
                "Segunda corrida no debe recalcular beneficios");
        assertCompara("0.00", segundo.dineroTotalCreditado());
    }

    /**
     * Los beneficios de activacion se generan SIEMPRE hasta 10 niveles hacia arriba,
     * aunque el beneficiario no cobre todavia (quedan registrados para el pago retroactivo).
     */
    @Test
    void beneficios_seGeneranHasta10NivelesAunqueNoCobren() {
        Persona comprador = personaDao.save(Persona.builder().nombres("Ben10").apellidos("Comprador").build());
        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();

        // Cadena de 10 personas encima del comprador: u1 (nivel 1) ... u10 (nivel 10)
        // Cada referido registra: persona -> su patrocinador
        Persona anterior = comprador;
        List<Persona> uplines = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Persona u = personaDao.save(Persona.builder().nombres("Ben10").apellidos("Nivel" + i).build());
            referidoDao.save(referido(anterior, u, plan, ahora.minusDays(30), ahora.minusDays(1), false));
            uplines.add(u);
            anterior = u;
        }

        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-BEN10-" + System.nanoTime())
                .nombre("Producto beneficios 10 niveles")
                .precio(new BigDecimal("100.00"))
                .precioPublico(new BigDecimal("100.00"))
                .pv(new BigDecimal("50.00"))
                .qp(new BigDecimal("25.00"))
                .cr(BigDecimal.ZERO)
                .build());

        Compra compra = compraService.registrarCompra(
                comprador.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 1)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        List<BeneficioActivacionCompra> filas = activosB(beneficioActivacionCompraDao.findByCompraId(compra.getId()));
        assertEquals(10, filas.size(), "Debe haber un registro de beneficio por cada uno de los 10 niveles");

        Set<Integer> niveles = new HashSet<>();
        filas.forEach(f -> niveles.add(f.getNivelGenerado()));
        for (int i = 1; i <= 10; i++) {
            assertTrue(niveles.contains(i), "Falta el registro del nivel " + i);
        }
        // Nadie tiene membresia activa: nada se paga, pero todo queda registrado
        assertTrue(filas.stream().noneMatch(BeneficioActivacionCompra::getPaga),
                "Sin membresia nadie cobra, pero los registros existen para el retroactivo");
    }

    /**
     * El rango suma niveles de alcance: Estandar (3) + rango con 1 nivel extra
     * permite cobrar tambien el nivel 4 con el mismo monto por producto.
     */
    @Test
    void rango_sumaNivelesDeAlcanceParaElBeneficio() {
        PlanActivacion estandar = planActivacionDao.findAll().stream()
                .filter(p -> Auditoria.ESTADO_ACTIVO.equals(p.getEstado()))
                .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase().contains("estandar"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe plan Estandar en la BD"));

        // Rango de prueba por ENCIMA de DIAMANTE (250000) para que sea el alcanzable
        Rango rangoConExtra = rangoDao.save(Rango.builder()
                .nombre("TEST-RANGO-EXTRA-" + System.nanoTime())
                .qpMinimo(new BigDecimal("500000.00"))
                .nivelesExtra(1)
                .build());

        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();
        var fechaFinPeriodo = gestionPeriodoService.obtenerPeriodoActivo().getFechaFin();
        LocalDateTime finMembresia = fechaFinPeriodo.atTime(23, 59, 59);

        Persona comprador = personaDao.save(Persona.builder().nombres("Rango").apellidos("Comprador").build());
        Persona n1 = personaDao.save(Persona.builder().nombres("Rango").apellidos("Nivel1").build());
        Persona n2 = personaDao.save(Persona.builder().nombres("Rango").apellidos("Nivel2").build());
        Persona n3 = personaDao.save(Persona.builder().nombres("Rango").apellidos("Nivel3").build());
        Persona n4 = personaDao.save(Persona.builder().nombres("Rango").apellidos("Nivel4EstandarMasRango").build());

        referidoDao.save(referido(comprador, n1, plan, ahora.minusDays(30), finMembresia, false));
        referidoDao.save(referido(n1, n2, plan, ahora.minusDays(30), finMembresia, false));
        referidoDao.save(referido(n2, n3, plan, ahora.minusDays(30), finMembresia, false));
        referidoDao.save(referido(n3, n4, plan, ahora.minusDays(30), finMembresia, false));

        // Cada upline: Estandar activo (200 PV propio), membresia vigente
        for (Persona u : List.of(n1, n2, n3)) {
            billeteraDao.save(billetera(u, new BigDecimal("200.00"), new BigDecimal("200.00")));
            Referido ru = referidoDao.findByPersonaId(u.getId()).orElseThrow();
            ru.setMembresiaActiva(true);
            referidoDao.save(ru);
        }
        // Nivel 4: Estandar + rango (+1 nivel extra) => alcance efectivo 4
        Billetera bN4 = billeteraDao.save(billetera(n4, new BigDecimal("200.00"), new BigDecimal("200.00")));
        // El rango se sostiene por QP: al acreditarle el volumen de red, el recalculo
        // de rango debe conservar (no resetear) un rango cuyo qp_minimo alcanza
        bN4.setSaldoQp(new BigDecimal("600000.00"));
        billeteraDao.save(bN4);
        n4.setRangoActual(rangoConExtra);
        personaDao.save(n4);
        // n4 necesita su propia fila de referido para tener estado de membresia
        Persona n5 = personaDao.save(Persona.builder().nombres("Rango").apellidos("Nivel5").build());
        referidoDao.save(referido(n4, n5, plan, ahora.minusDays(30), finMembresia, false));
        Referido r4 = referidoDao.findByPersonaId(n4.getId()).orElseThrow();
        r4.setMembresiaActiva(true);
        referidoDao.save(r4);

        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-RANGO-" + System.nanoTime())
                .nombre("Producto prueba rango suma niveles")
                .precio(new BigDecimal("150.00"))
                .precioPublico(new BigDecimal("150.00"))
                .pv(new BigDecimal("60.00"))
                .qp(new BigDecimal("30.00"))
                .cr(BigDecimal.ZERO)
                .build());

        Compra compra = compraService.registrarCompra(
                comprador.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 2)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        Map<Long, BeneficioActivacionCompra> benef = activosB(
                beneficioActivacionCompraDao.findByCompraId(compra.getId())).stream()
                .collect(toMap(b -> b.getBeneficiario().getId(), b -> b));

        // Niveles 1..3 pagan con el monto del nivel 1 de Estandar
        for (Persona u : List.of(n1, n2, n3)) {
            BeneficioActivacionCompra b = benef.get(u.getId());
            assertNotNull(b, "Falta beneficio para " + u.getApellidos());
            assertTrue(b.getPaga(), u.getApellidos() + " debe cobrar (nivel dentro del alcance)");
            assertCompara("12.00", b.getMontoTotal()); // 6.00 x 2 productos
        }
        // Nivel 4 paga SOLO gracias al rango (+1 nivel extra sobre alcance 3)
        BeneficioActivacionCompra b4 = benef.get(n4.getId());
        assertNotNull(b4, "Falta beneficio para el nivel 4");
        assertTrue(b4.getPaga(), "El rango debe extender el alcance y pagar el nivel 4");
        assertCompara("12.00", b4.getMontoTotal());
        assertCompara("12.00", b4.getMontoTotal());
    }

    /**
     * Subida Estandar -> Ultra a mitad de mes: el recálculo es retroactivo y
     * todas las recompensas del periodo pasan al monto Ultra (12.50).
     */
    @Test
    void subidaEstandarAUltra_recalculaRetroractivamenteLasRecompensas() {
        PlanActivacion ultra = planActivacionDao.findAll().stream()
                .filter(p -> Auditoria.ESTADO_ACTIVO.equals(p.getEstado()))
                .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase().contains("ultra"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe plan Ultra en la BD"));

        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();
        var fechaFinPeriodo = gestionPeriodoService.obtenerPeriodoActivo().getFechaFin();
        LocalDateTime finMembresia = fechaFinPeriodo.atTime(23, 59, 59);

        Persona comprador = personaDao.save(Persona.builder().nombres("Upgrade").apellidos("Comprador").build());
        Persona u1 = personaDao.save(Persona.builder().nombres("Upgrade").apellidos("Estandar").build());
        referidoDao.save(referido(comprador, u1, plan, ahora.minusDays(30), finMembresia, false));

        // Empieza el mes como Estandar (200 PV propio) con membresia activa
        billeteraDao.save(billetera(u1, new BigDecimal("200.00"), new BigDecimal("200.00")));
        // u1 necesita su propia fila de referido para tener estado de membresia
        Persona u0 = personaDao.save(Persona.builder().nombres("Upgrade").apellidos("Nivel2").build());
        referidoDao.save(referido(u1, u0, plan, ahora.minusDays(30), finMembresia, false));
        Referido r1 = referidoDao.findByPersonaId(u1.getId()).orElseThrow();
        r1.setMembresiaActiva(true);
        referidoDao.save(r1);

        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-UP-" + System.nanoTime())
                .nombre("Producto prueba upgrade")
                .precio(new BigDecimal("300.00"))
                .precioPublico(new BigDecimal("300.00"))
                .pv(new BigDecimal("210.00"))
                .qp(new BigDecimal("90.00"))
                .cr(BigDecimal.ZERO)
                .build());

        Compra compra = compraService.registrarCompra(
                comprador.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 2)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        // Como Estandar cobra 6.00 x 2 = 12.00 en el nivel 1
        BeneficioActivacionCompra beneficio = activosB(beneficioActivacionCompraDao.findByCompraId(compra.getId()))
                .stream()
                .filter(b -> b.getBeneficiario().getId().equals(u1.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(beneficio.getPaga());
        assertCompara("12.00", beneficio.getMontoTotal());

        // A mitad de mes cruza el umbral Ultra (PV propio 400): recálculo retroactivo
        Billetera b1 = billeteraDao.findByPersonaId(u1.getId()).orElseThrow();
        b1.setSaldoPvPropio(new BigDecimal("400.00"));
        billeteraDao.save(b1);
        billeteraService.recalcularBeneficiosActivacion(personaDao.findById(u1.getId()).orElseThrow());

        BeneficioActivacionCompra despues = activosB(beneficioActivacionCompraDao.findByCompraId(compra.getId()))
                .stream()
                .filter(b -> b.getBeneficiario().getId().equals(u1.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(despues.getPaga());
        assertCompara("12.50", despues.getMontoPorProducto(), "Todas sus recompensas pasan a monto Ultra");
        assertCompara("25.00", despues.getMontoTotal());

        // Ajuste retroactivo: 25.00 - 12.00 = 13.00 acreditados
        List<MovimientoBilletera> ajustes = movimientoBilleteraDao
                .findByBilleteraPersonaIdAndPeriodoIdOrderByFechaRegistroDesc(
                        u1.getId(), gestionPeriodoService.obtenerPeriodoActivo().getId()).stream()
                .filter(m -> Auditoria.ESTADO_ACTIVO.equals(m.getEstado()))
                .filter(m -> m.getConcepto() != null && m.getConcepto().startsWith("Ajuste retroactivo"))
                .toList();
        assertEquals(1, ajustes.size());
        assertCompara("13.00", ajustes.get(0).getMonto());
    }

    /**
     * El recreador resetea las recompensas de cada compra y las regenera con la
     * logica vigente: un upline sin membresia que se activa despues pasa a cobrar
     * retroactivamente por su nivel, y el volumen de red no se duplica.
     */
    @Test
    void recrearRecompensas_regeneraBeneficiosYNoDuplicaRed() {
        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();
        var fechaFinPeriodo = gestionPeriodoService.obtenerPeriodoActivo().getFechaFin();
        LocalDateTime finMembresia = fechaFinPeriodo.atTime(23, 59, 59);

        Persona comprador = personaDao.save(Persona.builder().nombres("Rec").apellidos("Comprador").build());
        Persona u1 = personaDao.save(Persona.builder().nombres("Rec").apellidos("EstandarActiva").build());
        Persona u2 = personaDao.save(Persona.builder().nombres("Rec").apellidos("SinMembresia").build());
        referidoDao.save(referido(comprador, u1, plan, ahora.minusDays(30), finMembresia, false));
        referidoDao.save(referido(u1, u2, plan, ahora.minusDays(30), finMembresia, false));
        referidoDao.save(referido(u2, null, plan, ahora.minusDays(30), finMembresia, false));

        billeteraDao.save(billetera(u1, new BigDecimal("200.00"), new BigDecimal("200.00")));
        Referido r1 = referidoDao.findByPersonaId(u1.getId()).orElseThrow();
        r1.setMembresiaActiva(true);
        referidoDao.save(r1);
        billeteraDao.save(billetera(u2, BigDecimal.ZERO, BigDecimal.ZERO));

        Producto producto = productoDao.save(Producto.builder()
                .sku("TEST-REC-" + System.nanoTime())
                .nombre("Producto prueba recreacion")
                .precio(new BigDecimal("300.00"))
                .precioPublico(new BigDecimal("300.00"))
                .pv(new BigDecimal("50.00"))
                .qp(new BigDecimal("20.00"))
                .cr(BigDecimal.ZERO)
                .build());

        Compra compra = compraService.registrarCompra(
                comprador.getId(),
                List.of(new CompraService.ItemCompraRequest(producto.getId(), 2)),
                pagoEfectivo());
        compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST");

        Map<Long, BeneficioActivacionCompra> antes = activosB(
                beneficioActivacionCompraDao.findByCompraId(compra.getId())).stream()
                .collect(toMap(b -> b.getBeneficiario().getId(), b -> b));
        assertTrue(antes.get(u1.getId()).getPaga());
        assertFalse(antes.get(u2.getId()).getPaga(), "Sin membresia u2 no cobra inicialmente");
        int redAntes = activos(movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaId(REF_RED, compra.getId())).size();

        // u2 se activa con Ultra a mitad de mes
        Billetera b2 = billeteraDao.findByPersonaId(u2.getId()).orElseThrow();
        b2.setSaldoPvPropio(new BigDecimal("400.00"));
        billeteraDao.save(b2);
        Referido r2 = referidoDao.findByPersonaId(u2.getId()).orElseThrow();
        r2.setMembresiaActiva(true);
        referidoDao.save(r2);

        var resumen = reprocesoService.recrearRecompensas(false);
        assertFalse(resumen.simulacion());
        assertTrue(resumen.comprasProcesadas() >= 1, "Debe haber procesado al menos la compra de prueba");

        Map<Long, BeneficioActivacionCompra> despues = activosB(
                beneficioActivacionCompraDao.findByCompraId(compra.getId())).stream()
                .collect(toMap(b -> b.getBeneficiario().getId(), b -> b));
        assertTrue(despues.get(u1.getId()).getPaga());
        assertCompara("12.00", despues.get(u1.getId()).getMontoTotal());

        // u2 ahora cobra retroactivo como Ultra: 12.50 x 2 productos
        BeneficioActivacionCompra b2Despues = despues.get(u2.getId());
        assertNotNull(b2Despues, "u2 debe tener fila de beneficio regenerada");
        assertTrue(b2Despues.getPaga(), "Con membresia Ultra activa u2 debe cobrar retroactivo");
        assertCompara("25.00", b2Despues.getMontoTotal());

        int redDespues = activos(movimientoBilleteraDao
                .findByReferenciaTipoAndReferenciaId(REF_RED, compra.getId())).size();
        assertEquals(redAntes, redDespues, "El recreador no debe duplicar el volumen de red");
    }

    /**
     * Regla de equidad desde rangos superiores a 25.000 QP: el QP contable por
     * rama directa se topa a objetivo/numeroDeDirectos y todas las ramas aportan.
     * Con 60.000 QP propios en una sola rama NO se alcanza ESMERALDA (50.000).
     */
    @Test
    void reglaRangosAltos_elQpSeTopePorRamaDirecta() {
        Rango esmeralda = rangoDao.findAll().stream()
                .filter(r -> Auditoria.ESTADO_ACTIVO.equals(r.getEstado()))
                .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase("ESMERALDA"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe rango ESMERALDA"));

        Persona p = personaDao.save(Persona.builder().nombres("Regla").apellidos("Padre").build());
        Billetera bp = billeteraDao.save(billetera(p, BigDecimal.ZERO, BigDecimal.ZERO));
        bp.setSaldoQp(new BigDecimal("60000.00"));
        billeteraDao.save(bp);

        LocalDateTime ahora = LocalDateTime.now();
        Plan plan = primerPlan();
        // 5 ramas directas: 4 con 10.000 QP y una con 0
        List<Persona> directos = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Persona d = personaDao.save(Persona.builder().nombres("Regla").apellidos("Rama" + i).build());
            Billetera bd = billeteraDao.save(billetera(d, BigDecimal.ZERO, BigDecimal.ZERO));
            bd.setSaldoQp(new BigDecimal("10000.00"));
            billeteraDao.save(bd);
            referidoDao.save(referido(d, p, plan, ahora.minusDays(30), ahora.plusDays(30), false));
            directos.add(d);
        }
        Persona d5 = personaDao.save(Persona.builder().nombres("Regla").apellidos("Rama5").build());
        referidoDao.save(referido(d5, p, plan, ahora.minusDays(30), ahora.plusDays(30), false));

        var progreso = billeteraService.calcularProgresoRangos(p.getId());
        var infoEsmeralda = progreso.rangos().stream()
                .filter(r -> "ESMERALDA".equals(r.nombre()))
                .findFirst()
                .orElseThrow();
        assertTrue(infoEsmeralda.reglaDirectos(), "ESMERALDA debe aplicar la regla de directos");
        assertCompara("10000.00", infoEsmeralda.topePorRama()); // 50000 / 5
        assertCompara("40000.00", infoEsmeralda.qpEfectivo());  // 4x10000 + 0
        assertFalse(infoEsmeralda.cumple(), "Sin la quinta rama activa no alcanza ESMERALDA");

        // La rama faltante crece: ahora todas aportan y ESMERALDA se alcanza
        Billetera bd5 = billeteraDao.save(billetera(d5, BigDecimal.ZERO, BigDecimal.ZERO));
        bd5.setSaldoQp(new BigDecimal("12000.00"));
        billeteraDao.save(bd5);

        billeteraService.actualizarRangoActual(p, new BigDecimal("60000.00"));
        Persona pRecargada = personaDao.findById(p.getId()).orElseThrow();
        assertNotNull(pRecargada.getRangoActual(), "Debe alcanzar un rango");
        assertEquals(esmeralda.getId(), pRecargada.getRangoActual().getId(),
                "Con todas las ramas al tope debe lograr ESMERALDA aunque su QP propio sea de otra rama");
    }

    private Plan primerPlan() {
        return planDao.findAll().stream()
                .filter(p -> Auditoria.ESTADO_ACTIVO.equals(p.getEstado()))
                .findFirst()
                .orElse(null);
    }

    private void assertCompara(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                "Se esperaba " + esperado + " pero fue " + actual);
    }

    private void assertCompara(String esperado, BigDecimal actual, String mensaje) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                mensaje + " (se esperaba " + esperado + " pero fue " + actual + ")");
    }

    private List<BeneficioActivacionCompra> activosB(List<BeneficioActivacionCompra> list) {
        return list.stream().filter(b -> Auditoria.ESTADO_ACTIVO.equals(b.getEstado())).toList();
    }

    private List<MovimientoBilletera> activos(List<MovimientoBilletera> list) {
        return list.stream().filter(m -> Auditoria.ESTADO_ACTIVO.equals(m.getEstado())).toList();
    }

    private CompraService.PagoCompraRequest pagoEfectivo() {
        return new CompraService.PagoCompraRequest(
                "EFECTIVO", null, null, null, "TEST-REF", null, null, null, null, null);
    }

    private Billetera billetera(Persona p, BigDecimal saldoPv, BigDecimal saldoPvPropio) {
        return Billetera.builder()
                .persona(p)
                .saldoDinero(BigDecimal.ZERO)
                .saldoPv(saldoPv)
                .saldoPvPropio(saldoPvPropio)
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
