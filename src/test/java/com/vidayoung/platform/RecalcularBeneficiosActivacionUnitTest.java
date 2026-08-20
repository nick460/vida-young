package com.vidayoung.platform;

import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.HistorialMembresiaDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
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
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import com.vidayoung.platform.Model.Service.NotificacionService;
import com.vidayoung.platform.Model.ServiceImpl.BilleteraServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecalcularBeneficiosActivacionUnitTest {

    @Mock private BilleteraDao billeteraDao;
    @Mock private CierreMensualBilleteraDao cierreMensualBilleteraDao;
    @Mock private MovimientoBilleteraDao movimientoBilleteraDao;
    @Mock private HistorialMembresiaDao historialMembresiaDao;
    @Mock private PersonaDao personaDao;
    @Mock private PlanActivacionDao planActivacionDao;
    @Mock private PlanActivacionNivelDao planActivacionNivelDao;
    @Mock private PlanDao planDao;
    @Mock private ProductoDao productoDao;
    @Mock private RangoDao rangoDao;
    @Mock private RecompensaDao recompensaDao;
    @Mock private ReferidoDao referidoDao;
    @Mock private BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    @Mock private CarteraEmpresaService carteraEmpresaService;
    @Mock private GestionPeriodoService gestionPeriodoService;
    @Mock private RetiroBilleteraDao retiroBilleteraDao;
    @Mock private RetiroBilleteraDetalleDao retiroBilleteraDetalleDao;
    @Mock private NotificacionService notificacionService;

    private BilleteraServiceImpl service;
    private Persona persona;
    private PeriodoGestion periodo;

    @BeforeEach
    void setUp() {
        service = new BilleteraServiceImpl(
                billeteraDao, cierreMensualBilleteraDao, movimientoBilleteraDao, historialMembresiaDao,
                personaDao, planActivacionDao, planActivacionNivelDao, planDao, productoDao, rangoDao,
                recompensaDao, referidoDao, beneficioActivacionCompraDao, carteraEmpresaService,
                gestionPeriodoService, retiroBilleteraDao, retiroBilleteraDetalleDao, notificacionService);
        persona = Persona.builder().nombres("Prueba").apellidos("Unit").build();
        persona.setId(1L);
        periodo = PeriodoGestion.builder().id(1L).fechaFin(LocalDate.of(2026, 8, 31)).build();

        when(gestionPeriodoService.obtenerPeriodoActivo()).thenReturn(periodo);
    }

    @Test
    void ajustePositivoPorMembresiaSuperior() {
        BeneficioActivacionCompra beneficio = BeneficioActivacionCompra.builder()
                .id(100L)
                .nivelGenerado(1)
                .cantidadProductos(2)
                .montoPorProducto(BigDecimal.ZERO)
                .montoTotal(BigDecimal.ZERO)
                .paga(false)
                .build();
        Billetera billetera = billetera(new BigDecimal("400.00"), BigDecimal.ZERO);
        planUltra();

        when(beneficioActivacionCompraDao.findByBeneficiarioIdAndPeriodoId(1L, 1L))
                .thenReturn(List.of(beneficio));
        when(billeteraDao.findByPersonaId(1L)).thenReturn(Optional.of(billetera));
        when(referidoDao.findByPersonaId(1L)).thenReturn(Optional.of(referidoActivo()));
        when(beneficioActivacionCompraDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billeteraDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recalcularBeneficiosActivacion(persona);

        ArgumentCaptor<MovimientoBilletera> captor = ArgumentCaptor.forClass(MovimientoBilletera.class);
        verify(movimientoBilleteraDao).save(captor.capture());
        MovimientoBilletera movimiento = captor.getValue();
        assertCompara("25.00", movimiento.getMonto());
        assertEquals("ACTUALIZACION_BENEFICIO_ACTIVACION", movimiento.getReferenciaTipo());
        assertEquals(100L, movimiento.getReferenciaId());
        assertTrue(movimiento.getConcepto().startsWith("Ajuste retroactivo"));
        assertTrue(movimiento.getConcepto().contains("membresia superior"));
        assertCompara("25.00", movimiento.getSaldoResultado());
        assertCompara("25.00", billetera.getSaldoDinero());
        assertTrue(beneficio.getPaga());
        assertCompara("12.50", beneficio.getMontoPorProducto());
        assertCompara("25.00", beneficio.getMontoTotal());
    }

    @Test
    void ajusteNegativoRechazadoCuandoSaldoYaFueUtilizado() {
        BeneficioActivacionCompra beneficio = BeneficioActivacionCompra.builder()
                .id(200L)
                .nivelGenerado(1)
                .cantidadProductos(2)
                .montoPorProducto(new BigDecimal("50.00"))
                .montoTotal(new BigDecimal("100.00"))
                .paga(true)
                .build();
        Billetera billetera = billetera(new BigDecimal("400.00"), BigDecimal.ZERO);
        planUltra();

        when(beneficioActivacionCompraDao.findByBeneficiarioIdAndPeriodoId(1L, 1L))
                .thenReturn(List.of(beneficio));
        when(billeteraDao.findByPersonaId(1L)).thenReturn(Optional.of(billetera));
        when(referidoDao.findByPersonaId(1L)).thenReturn(Optional.of(referidoActivo()));

        assertThrows(IllegalArgumentException.class,
                () -> service.recalcularBeneficiosActivacion(persona),
                "No se puede ajustar el beneficio porque el saldo en dinero ya fue utilizado");
    }

    @Test
    void sinAjusteCuandoBeneficioYaCoincideConPlanActual() {
        BeneficioActivacionCompra beneficio = BeneficioActivacionCompra.builder()
                .id(300L)
                .nivelGenerado(1)
                .cantidadProductos(2)
                .montoPorProducto(new BigDecimal("12.50"))
                .montoTotal(new BigDecimal("25.00"))
                .paga(true)
                .build();
        Billetera billetera = billetera(new BigDecimal("400.00"), new BigDecimal("25.00"));
        planUltra();

        when(beneficioActivacionCompraDao.findByBeneficiarioIdAndPeriodoId(1L, 1L))
                .thenReturn(List.of(beneficio));
        when(billeteraDao.findByPersonaId(1L)).thenReturn(Optional.of(billetera));
        when(referidoDao.findByPersonaId(1L)).thenReturn(Optional.of(referidoActivo()));

        service.recalcularBeneficiosActivacion(persona);

        verify(movimientoBilleteraDao, never()).save(any());
        assertCompara("25.00", billetera.getSaldoDinero());
    }

    private void planUltra() {
        PlanActivacion ultra = PlanActivacion.builder()
                .id(2L)
                .nombre("Activacion Ultra")
                .pvMinimoMensual(new BigDecimal("400"))
                .nivelesAlcance(4)
                .build();
        PlanActivacionNivel nivel = PlanActivacionNivel.builder()
                .numeroNivel(1)
                .montoPorProducto(new BigDecimal("12.50"))
                .build();
        when(planActivacionDao.findByPvMinimoMensualLessThanEqualOrderByPvMinimoMensualDesc(any()))
                .thenReturn(List.of(ultra));
        when(planActivacionNivelDao.findByPlanActivacionIdAndNumeroNivel(2L, 1))
                .thenReturn(Optional.of(nivel));
    }

    private Referido referidoActivo() {
        return Referido.builder()
                .membresiaActiva(true)
                .fechaFinMembresia(LocalDateTime.of(2026, 8, 31, 23, 59, 59))
                .build();
    }

    private Billetera billetera(BigDecimal saldoPv, BigDecimal saldoDinero) {
        return Billetera.builder()
                .persona(persona)
                .saldoPv(saldoPv)
                .saldoDinero(saldoDinero)
                .saldoQp(BigDecimal.ZERO)
                .saldoCr(BigDecimal.ZERO)
                .saldoProductos(BigDecimal.ZERO)
                .build();
    }

    private void assertCompara(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                "Se esperaba " + esperado + " pero fue " + actual);
    }
}