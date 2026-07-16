package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.CompraPublica;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.ProductoDescuentoCliente;
import com.vidayoung.platform.Model.Entity.TipoClientePublico;
import com.vidayoung.platform.Model.Entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TiendaPublicaService {

    Optional<Usuario> buscarDistribuidor(String username);

    List<TipoClientePublico> listarTiposCliente();

    List<ProductoDescuentoCliente> listarDescuentos(Long tipoClienteId);

    Producto guardarProducto(Producto producto);

    ProductoDescuentoCliente guardarDescuento(Long productoId, Long tipoClienteId, BigDecimal descuentoMonto);

    List<ProductoPublicoResponse> listarProductosPublicos(String username, String tipoClienteCodigo);

    CompraPublica registrarCompraPublica(String username, CompraPublicaRequest request);

    List<CompraPublica> listarComprasPublicas();

    List<ClientePublicoAdminResponse> listarClientesPublicos(Long distribuidorId, Long tipoClienteId);

    CompraPublica cambiarEstado(Long compraId, String estadoCompra, String usuarioOperacion);

    Optional<ClientePublicoResponse> buscarClientePorDocumento(String documento);

    Optional<ClientePublicoResponse> buscarClientePorDocumento(String username, String documento);

    record ProductoPublicoResponse(
            Long id,
            String sku,
            String nombre,
            String descripcion,
            String categoria,
            String imagenUrl,
            BigDecimal precioDistribuidor,
            BigDecimal precioPublico,
            BigDecimal descuento,
            BigDecimal precioFinal,
            String tipoClienteCodigo,
            String tipoClienteNombre
    ) {
    }

    record ItemCompraPublicaRequest(Long productoId, Integer cantidad) {
    }

    record ClientePublicoResponse(
            String nombres,
            String apellidos,
            String documento,
            String email,
            String telefono,
            Boolean envioRequiere,
            String envioDireccion,
            String envioCiudad,
            String envioReferencia,
            String origen
    ) {
    }

    record ClientePublicoAdminResponse(
            Long id,
            Long distribuidorId,
            String distribuidorNombre,
            Long tipoClienteId,
            String tipoClienteCodigo,
            String tipoClienteNombre,
            String nombres,
            String apellidos,
            String documento,
            String email,
            String telefono,
            String envioDireccion,
            String envioCiudad,
            String envioReferencia,
            LocalDateTime fechaRegistro
    ) {
    }

    record CompraPublicaRequest(
            List<ItemCompraPublicaRequest> items,
            String tipoClienteCodigo,
            String clienteNombres,
            String clienteApellidos,
            String clienteDocumento,
            String clienteEmail,
            String clienteTelefono,
            Boolean envioRequiere,
            String envioDireccion,
            String envioCiudad,
            String envioReferencia,
            String metodoPago,
            String referenciaPago,
            String comprobantePagoUrl,
            String comprobantePagoNombre,
            String comprobantePagoTipo
    ) {
    }
}
