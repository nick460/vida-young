package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Compra;
import java.util.List;

public interface CompraService {

    Compra registrarCompra(Long personaId, List<ItemCompraRequest> items, PagoCompraRequest pago);

    Compra modificarCompra(Long compraId, List<ItemCompraRequest> items, PagoCompraRequest pago);

    List<Compra> listarPorPersona(Long personaId);

    List<Compra> listarPorPersonaYPeriodo(Long personaId, Long periodoId);

    List<Compra> listarPorEstado(String estadoCompra);

    List<Compra> listarTodas();

    List<Compra> listarPorPeriodo(Long periodoId);

    List<BeneficioActivacionCompra> listarBeneficiosPorCompra(Long compraId);

    Compra cambiarEstado(Long compraId, String estadoCompra, String usuarioOperacion);

    Compra anularCompra(Long compraId, String motivo, String usuarioOperacion);

    int reprocesarQpBonoReferido(Long compraId, boolean notificar);

    List<MovimientoCompraResumen> listarMovimientosCompra(Long compraId);

    record ItemCompraRequest(Long productoId, Integer cantidad) {
    }

    record MovimientoCompraResumen(
            String origen,
            Long personaId,
            String personaNombres,
            String personaApellidos,
            String tipo,
            String concepto,
            java.math.BigDecimal monto,
            java.math.BigDecimal saldoResultado,
            java.time.LocalDateTime fechaRegistro,
            Integer nivel
    ) {
    }

    record PagoCompraRequest(
            String metodoPago,
            String bancoPago,
            String cuentaPago,
            String codigoPago,
            String referenciaPago,
            java.math.BigDecimal descuentoMonto,
            String descuentoConcepto,
            String comprobantePagoUrl,
            String comprobantePagoNombre,
            String comprobantePagoTipo
    ) {
    }
}
