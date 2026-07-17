package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.Compra;
import java.util.List;

public interface CompraService {

    Compra registrarCompra(Long personaId, List<ItemCompraRequest> items, PagoCompraRequest pago);

    List<Compra> listarPorPersona(Long personaId);

    List<Compra> listarPorEstado(String estadoCompra);

    List<Compra> listarTodas();

    List<Compra> listarPorPeriodo(Long periodoId);

    List<BeneficioActivacionCompra> listarBeneficiosPorCompra(Long compraId);

    Compra cambiarEstado(Long compraId, String estadoCompra, String usuarioOperacion);

    record ItemCompraRequest(Long productoId, Integer cantidad) {
    }

    record PagoCompraRequest(
            String metodoPago,
            String bancoPago,
            String cuentaPago,
            String codigoPago,
            String referenciaPago,
            String comprobantePagoUrl,
            String comprobantePagoNombre,
            String comprobantePagoTipo
    ) {
    }
}
