package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BilleteraService {

    int NIVELES_TOTALES = 10;

    int calcularAlcanceEfectivo(Persona persona, PlanActivacion plan);

    ProgresoRangosResponse calcularProgresoRangos(Long personaId);

    record RamaProgresoResponse(String nombrePersona, BigDecimal qpRama, BigDecimal qpContable) {
    }

    record RangoProgresoResponse(
            Long rangoId,
            String nombre,
            BigDecimal qpMinimo,
            boolean reglaDirectos,
            Integer numeroDirectos,
            BigDecimal topePorRama,
            BigDecimal qpEfectivo,
            boolean cumple,
            List<RamaProgresoResponse> ramas
    ) {
    }

    record ProgresoRangosResponse(
            BigDecimal qpTotal,
            String rangoActual,
            String rangoSiguiente,
            List<RangoProgresoResponse> rangos
    ) {
    }

    Billetera asegurarBilletera(Persona persona);

    Optional<Billetera> buscarPorPersonaId(Long personaId);

    List<Billetera> listarBilleterasConSaldos();

    List<MovimientoBilletera> listarMovimientos(Long personaId);

    List<HistorialMembresia> listarHistorialMembresias(Long personaId);

    List<CierreMensualBilletera> listarCierresMensuales(Long personaId);

    void actualizarRangoActual(Persona persona, java.math.BigDecimal qpActual);

    void registrarAfiliacionInicial(Referido referido);

    HistorialMembresia registrarActivacion(Long personaId, Long planId);

    void activarMembresiaPorPv(Persona persona, BigDecimal pvActual, PeriodoGestion periodo);

    void recalcularBeneficiosActivacion(Persona persona);

    void recalcularBeneficiosActivacion(Persona persona, boolean notificar);

    RetiroBilletera registrarRetiro(Long personaId, BigDecimal montoDinero, BigDecimal montoProductos, List<ProductoRetiroRequest> productos, String observacion);

    RetiroBilletera registrarRetiro(Long personaId, Long periodoId, BigDecimal montoDinero, BigDecimal montoProductos, List<ProductoRetiroRequest> productos, String observacion);

    void sincronizarSaldoProductosRecompensa(Long recompensaId);

    int vencerHistorialMembresiasExpiradas();

    int vencerHistorialMembresiasActivas();

    int cerrarMesBilleteras();

    PeriodoGestion cerrarPeriodoActivoPagado();

    record ProductoRetiroRequest(Long productoId, Integer cantidad) {
    }
}
