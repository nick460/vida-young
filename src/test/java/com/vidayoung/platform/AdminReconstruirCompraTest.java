package com.vidayoung.platform;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Service.CompraService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminReconstruirCompraTest {

    @Autowired private CompraService compraService;
    @Autowired private CompraDao compraDao;
    @Autowired private ProductoDao productoDao;
    @Autowired private PersonaDao personaDao;
    @Autowired private BilleteraDao billeteraDao;
    @Autowired private MovimientoBilleteraDao movimientoBilleteraDao;
    @Autowired private BeneficioActivacionCompraDao beneficioActivacionCompraDao;

    private Compra crearCompraValidada(Long personaId, Producto producto, int cantidad) {
        Compra compra = compraService.registrarCompra(
                personaId,
                List.of(new CompraService.ItemCompraRequest(producto.getId(), cantidad)),
                pagoEfectivo());
        return compraService.cambiarEstado(compra.getId(), Compra.ESTADO_COMPRA_VALIDADA, "TEST_ADMIN");
    }

    @Test
    void reconstruirCompraValidada_conNuevosProductos_yDescuento_dejaAnulada_yPreservaFecha() {
        Persona persona = personaDao.save(Persona.builder().nombres("Admin").apellidos("TestRebuild").build());
        // asegurar referido para que no falle por falta de persona? no necesario para compra
        Producto p1 = productoDao.save(Producto.builder()
                .sku("ADM-REBUILD-1-" + System.nanoTime())
                .nombre("Prod Admin 1")
                .precio(new BigDecimal("100.00"))
                .precioPublico(new BigDecimal("100.00"))
                .pv(new BigDecimal("10.00"))
                .qp(new BigDecimal("5.00"))
                .qpBonoReferido(BigDecimal.ZERO)
                .cr(BigDecimal.ZERO)
                .build());
        Producto p2 = productoDao.save(Producto.builder()
                .sku("ADM-REBUILD-2-" + System.nanoTime())
                .nombre("Prod Admin 2")
                .precio(new BigDecimal("200.00"))
                .precioPublico(new BigDecimal("200.00"))
                .pv(new BigDecimal("20.00"))
                .qp(new BigDecimal("10.00"))
                .qpBonoReferido(new BigDecimal("5.00"))
                .cr(BigDecimal.ZERO)
                .build());

        Compra original = crearCompraValidada(persona.getId(), p1, 2);
        Long originalId = original.getId();
        var fechaCompraOrig = original.getFechaCompra();
        var periodoOrig = original.getPeriodo().getId();
        var fechaValidacionOrig = original.getFechaValidacion();

        // Verificar que tiene movimientos
        assertFalse(movimientoBilleteraDao.findByReferenciaTipoAndReferenciaId("COMPRA", originalId).isEmpty());

        Compra nueva = compraService.reconstruirCompraAdmin(
                originalId,
                List.of(new CompraService.ItemCompraRequest(p2.getId(), 1)),
                new CompraService.PagoCompraRequest("EFECTIVO", null, null, null, "TEST-REBUILD", new BigDecimal("10.00"), "Descuento admin", null, null, null),
                "admin_test");

        assertNotNull(nueva);
        assertNotEquals(originalId, nueva.getId(), "Debe crear nueva compra con distinto id");
        assertEquals(Compra.ESTADO_COMPRA_VALIDADA, nueva.getEstadoCompra());
        assertEquals(fechaCompraOrig, nueva.getFechaCompra(), "Debe preservar misma fechaCompra");
        assertEquals(periodoOrig, nueva.getPeriodo().getId(), "Debe preservar mismo periodo");
        assertEquals(fechaValidacionOrig, nueva.getFechaValidacion(), "Debe preservar fechaValidacion");
        assertEquals(1, nueva.getDetalles().size());
        assertEquals(new BigDecimal("190.00"), nueva.getSubtotal()); // 200 -10
        assertTrue(nueva.getSubtotal().compareTo(original.getSubtotal()) != 0);

        Compra viejaRecargada = compraDao.findById(originalId).orElseThrow();
        assertEquals(Compra.ESTADO_COMPRA_ANULADA, viejaRecargada.getEstadoCompra());
        assertTrue(viejaRecargada.getMotivoAnulacion().contains("Edición administrativa"));
        assertEquals("admin_test", viejaRecargada.getUsuarioAnulacion());

        // La nueva debe tener movimientos de volumen
        assertFalse(movimientoBilleteraDao.findByReferenciaTipoAndReferenciaId("COMPRA", nueva.getId()).isEmpty(),
                "La nueva compra validada debe generar volumen");

        // La vieja debe tener sus movimientos anulados (ELIMINADO) y con movimiento de anulacion
        long activosVieja = movimientoBilleteraDao.findByReferenciaTipoAndReferenciaId("COMPRA", originalId).stream()
                .filter(m -> Auditoria.ESTADO_ACTIVO.equals(m.getEstado())).count();
        assertEquals(0, activosVieja, "Los movimientos de la vieja deben quedar ELIMINADO");
    }

    @Test
    void reconstruirCompraPendiente_preservaEstadoPendiente_yNoGeneraRecompensas() {
        Persona persona = personaDao.save(Persona.builder().nombres("Admin").apellidos("Pendiente").build());
        Producto p = productoDao.save(Producto.builder()
                .sku("ADM-PEND-" + System.nanoTime())
                .nombre("Prod Pend")
                .precio(new BigDecimal("50.00"))
                .precioPublico(new BigDecimal("50.00"))
                .pv(new BigDecimal("5.00"))
                .qp(new BigDecimal("5.00"))
                .qpBonoReferido(BigDecimal.ZERO)
                .cr(BigDecimal.ZERO)
                .build());
        Compra pendiente = compraService.registrarCompra(
                persona.getId(),
                List.of(new CompraService.ItemCompraRequest(p.getId(), 1)),
                pagoEfectivo());
        assertEquals(Compra.ESTADO_COMPRA_PENDIENTE, pendiente.getEstadoCompra());
        var fechaOrig = pendiente.getFechaCompra();
        var periodoOrig = pendiente.getPeriodo().getId();

        Producto p2 = productoDao.save(Producto.builder()
                .sku("ADM-PEND-2-" + System.nanoTime())
                .nombre("Prod Pend 2")
                .precio(new BigDecimal("80.00"))
                .precioPublico(new BigDecimal("80.00"))
                .pv(new BigDecimal("8.00"))
                .qp(new BigDecimal("8.00"))
                .qpBonoReferido(BigDecimal.ZERO)
                .cr(BigDecimal.ZERO)
                .build());

        Compra nueva = compraService.reconstruirCompraAdmin(
                pendiente.getId(),
                List.of(new CompraService.ItemCompraRequest(p2.getId(), 2)),
                pagoEfectivo(),
                "admin_test");

        assertEquals(Compra.ESTADO_COMPRA_PENDIENTE, nueva.getEstadoCompra(), "Pendiente debe seguir pendiente");
        assertEquals(fechaOrig, nueva.getFechaCompra());
        assertEquals(periodoOrig, nueva.getPeriodo().getId());
        assertEquals(2, nueva.getDetalles().size() == 1 ? nueva.getDetalles().get(0).getCantidad() : -1); // 2 unidades de p2
        // Pendiente no genera movimientos
        assertTrue(movimientoBilleteraDao.findByReferenciaTipoAndReferenciaId("COMPRA", nueva.getId()).isEmpty());

        Compra vieja = compraDao.findById(pendiente.getId()).orElseThrow();
        assertEquals(Compra.ESTADO_COMPRA_ANULADA, vieja.getEstadoCompra());
    }

    @Test
    void reconstruirConItemsVacios_debeFallar() {
        Persona persona = personaDao.save(Persona.builder().nombres("Admin").apellidos("Fail").build());
        Producto p = productoDao.save(Producto.builder()
                .sku("ADM-FAIL-" + System.nanoTime())
                .nombre("Prod Fail")
                .precio(new BigDecimal("10.00"))
                .precioPublico(new BigDecimal("10.00"))
                .pv(BigDecimal.ONE)
                .qp(BigDecimal.ONE)
                .qpBonoReferido(BigDecimal.ZERO)
                .cr(BigDecimal.ZERO)
                .build());
        Compra compra = crearCompraValidada(persona.getId(), p, 1);
        assertThrows(IllegalArgumentException.class, () ->
                compraService.reconstruirCompraAdmin(compra.getId(), List.of(), pagoEfectivo(), "admin"));
    }

    @Test
    void reconstruirCompraYaAnulada_debeFallar() {
        Persona persona = personaDao.save(Persona.builder().nombres("Admin").apellidos("Anulada").build());
        Producto p = productoDao.save(Producto.builder()
                .sku("ADM-ANUL-" + System.nanoTime())
                .nombre("Prod Anul")
                .precio(new BigDecimal("10.00"))
                .precioPublico(new BigDecimal("10.00"))
                .pv(BigDecimal.ONE)
                .qp(BigDecimal.ONE)
                .qpBonoReferido(BigDecimal.ZERO)
                .cr(BigDecimal.ZERO)
                .build());
        Compra compra = crearCompraValidada(persona.getId(), p, 1);
        compraService.anularCompra(compra.getId(), "test anulacion", "admin");
        assertThrows(IllegalArgumentException.class, () ->
                compraService.reconstruirCompraAdmin(compra.getId(),
                        List.of(new CompraService.ItemCompraRequest(p.getId(), 1)),
                        pagoEfectivo(), "admin"));
    }

    @Test
    void listarTodasAdmin_debeIncluirAnuladas() {
        // Crear una compra y anularla, luego verificar que listarTodas la incluye
        Persona persona = personaDao.save(Persona.builder().nombres("Admin").apellidos("Listar").build());
        Producto p = productoDao.save(Producto.builder()
                .sku("ADM-LIST-" + System.nanoTime())
                .nombre("Prod List")
                .precio(new BigDecimal("10.00"))
                .precioPublico(new BigDecimal("10.00"))
                .pv(BigDecimal.ONE)
                .qp(BigDecimal.ONE)
                .qpBonoReferido(BigDecimal.ZERO)
                .cr(BigDecimal.ZERO)
                .build());
        Compra compra = crearCompraValidada(persona.getId(), p, 1);
        compraService.anularCompra(compra.getId(), "para listar", "admin");
        var todas = compraService.listarTodas();
        assertTrue(todas.stream().anyMatch(c -> c.getId().equals(compra.getId()) && Compra.ESTADO_COMPRA_ANULADA.equals(c.getEstadoCompra())),
                "listarTodas debe incluir la compra anulada para auditoria");
    }

    private CompraService.PagoCompraRequest pagoEfectivo() {
        return new CompraService.PagoCompraRequest("EFECTIVO", null, null, null, "TEST-REF", null, null, null, null, null);
    }
}
