package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.RetiroBilletera;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BilleteraService {

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

    RetiroBilletera registrarRetiro(Long personaId, BigDecimal montoDinero, BigDecimal montoProductos, List<ProductoRetiroRequest> productos, String observacion);

    void sincronizarSaldoProductosRecompensa(Long recompensaId);

    int vencerHistorialMembresiasExpiradas();

    int vencerHistorialMembresiasActivas();

    int cerrarMesBilleteras();

    PeriodoGestion cerrarPeriodoActivoPagado();

    record ProductoRetiroRequest(Long productoId, Integer cantidad) {
    }
}
