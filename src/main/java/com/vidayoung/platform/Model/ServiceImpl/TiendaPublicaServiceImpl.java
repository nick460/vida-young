package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.ClientePublicoDao;
import com.vidayoung.platform.Model.Dao.CompraPublicaDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Dao.ProductoDescuentoClienteDao;
import com.vidayoung.platform.Model.Dao.TipoClientePublicoDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.ClientePublico;
import com.vidayoung.platform.Model.Entity.CompraPublica;
import com.vidayoung.platform.Model.Entity.CompraPublicaDetalle;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.ProductoDescuentoCliente;
import com.vidayoung.platform.Model.Entity.TipoClientePublico;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.ProductoService;
import com.vidayoung.platform.Model.Service.TiendaPublicaService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TiendaPublicaServiceImpl implements TiendaPublicaService {

    private static final String TIPO_CLIENTE_NORMAL = "NORMAL";
    private static final String REFERENCIA_VENTA_PUBLICA = "VENTA_PUBLICA";
    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            CompraPublica.ESTADO_PENDIENTE,
            CompraPublica.ESTADO_VALIDADA,
            CompraPublica.ESTADO_ENTREGADA,
            CompraPublica.ESTADO_RECHAZADA
    );

    private final UsuarioDao usuarioDao;
    private final ProductoDao productoDao;
    private final ProductoService productoService;
    private final TipoClientePublicoDao tipoClientePublicoDao;
    private final ProductoDescuentoClienteDao productoDescuentoClienteDao;
    private final CompraPublicaDao compraPublicaDao;
    private final ClientePublicoDao clientePublicoDao;
    private final BilleteraService billeteraService;
    private final BilleteraDao billeteraDao;
    private final MovimientoBilleteraDao movimientoBilleteraDao;

    @Override
    public Optional<Usuario> buscarDistribuidor(String username) {
        return usuarioDao.findByUsername(username)
                .filter(usuario -> Auditoria.ESTADO_ACTIVO.equals(usuario.getEstado()))
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()));
    }

    @Override
    public List<TipoClientePublico> listarTiposCliente() {
        return tipoClientePublicoDao.findAll().stream()
                .filter(tipo -> Auditoria.ESTADO_ACTIVO.equals(tipo.getEstado()))
                .toList();
    }

    @Override
    public List<ProductoDescuentoCliente> listarDescuentos(Long tipoClienteId) {
        return productoDescuentoClienteDao.findByTipoClienteId(tipoClienteId).stream()
                .filter(descuento -> Auditoria.ESTADO_ACTIVO.equals(descuento.getEstado()))
                .toList();
    }

    @Override
    public Producto guardarProducto(Producto producto) {
        if (producto.getPrecioPublico() == null || producto.getPrecioPublico().compareTo(BigDecimal.ZERO) <= 0) {
            producto.setPrecioPublico(producto.getPrecio());
        }
        return productoService.guardar(producto);
    }

    @Override
    public ProductoDescuentoCliente guardarDescuento(Long productoId, Long tipoClienteId, BigDecimal descuentoMonto) {
        Producto producto = productoDao.findById(productoId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        TipoClientePublico tipoCliente = tipoClientePublicoDao.findById(tipoClienteId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cliente no encontrado."));
        BigDecimal descuento = zeroIfNull(descuentoMonto);
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo.");
        }

        ProductoDescuentoCliente entity = productoDescuentoClienteDao
                .findByProductoIdAndTipoClienteId(productoId, tipoClienteId)
                .orElseGet(ProductoDescuentoCliente::new);
        entity.setProducto(producto);
        entity.setTipoCliente(tipoCliente);
        entity.setDescuentoMonto(descuento);
        entity.setEstado(Auditoria.ESTADO_ACTIVO);
        return productoDescuentoClienteDao.save(entity);
    }

    @Override
    public List<ProductoPublicoResponse> listarProductosPublicos(String username, String tipoClienteCodigo) {
        Usuario distribuidor = buscarDistribuidor(username)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada."));
        TipoClientePublico tipoCliente = resolverTipoCliente(tipoClienteCodigo);

        return productoService.listarParaShop().stream()
                .map(producto -> toProductoPublico(producto, tipoCliente))
                .toList();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public CompraPublica registrarCompraPublica(String username, CompraPublicaRequest request) {
        validarCompra(request);
        Usuario usuario = buscarDistribuidor(username)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada."));
        Persona distribuidor = usuario.getPersona();
        TipoClientePublico tipoCliente = resolverTipoCliente(request.tipoClienteCodigo());
        ClientePublico clientePublico = guardarClientePublico(distribuidor, tipoCliente, request);

        CompraPublica compra = CompraPublica.builder()
                .distribuidor(distribuidor)
                .tipoCliente(tipoCliente)
                .clientePublico(clientePublico)
                .fechaCompra(LocalDateTime.now())
                .estadoCompra(CompraPublica.ESTADO_PENDIENTE)
                .clienteNombres(normalizarTexto(request.clienteNombres()))
                .clienteApellidos(normalizarTexto(request.clienteApellidos()))
                .clienteDocumento(normalizarTexto(request.clienteDocumento()))
                .clienteEmail(normalizarTexto(request.clienteEmail()))
                .clienteTelefono(normalizarTexto(request.clienteTelefono()))
                .envioRequiere(Boolean.TRUE.equals(request.envioRequiere()))
                .envioDireccion(normalizarTexto(request.envioDireccion()))
                .envioCiudad(normalizarTexto(request.envioCiudad()))
                .envioReferencia(normalizarTexto(request.envioReferencia()))
                .metodoPago(normalizarTexto(request.metodoPago()))
                .referenciaPago(normalizarTexto(request.referenciaPago()))
                .comprobantePagoUrl(normalizarTexto(request.comprobantePagoUrl()))
                .comprobantePagoNombre(normalizarTexto(request.comprobantePagoNombre()))
                .comprobantePagoTipo(normalizarTexto(request.comprobantePagoTipo()))
                .build();
        compra = compraPublicaDao.save(compra);

        BigDecimal totalCliente = BigDecimal.ZERO;
        BigDecimal totalEmpresa = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;
        BigDecimal totalGanancia = BigDecimal.ZERO;

        for (ItemCompraPublicaRequest item : request.items()) {
            int cantidad = item.cantidad() == null ? 0 : item.cantidad();
            if (cantidad < 1) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
            }

            Producto producto = productoDao.findById(item.productoId())
                    .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                    .filter(found -> Boolean.TRUE.equals(found.getListarEnShop()))
                    .orElseThrow(() -> new IllegalArgumentException("Producto no disponible."));

            ProductoPublicoResponse calculo = toProductoPublico(producto, tipoCliente);
            BigDecimal qty = BigDecimal.valueOf(cantidad);
            BigDecimal subtotalCliente = calculo.precioFinal().multiply(qty);
            BigDecimal subtotalEmpresa = calculo.precioDistribuidor().multiply(qty);
            BigDecimal subtotalDescuento = calculo.descuento().multiply(qty);
            BigDecimal ganancia = subtotalCliente.subtract(subtotalEmpresa).max(BigDecimal.ZERO);

            compra.getDetalles().add(CompraPublicaDetalle.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioDistribuidorUnitario(calculo.precioDistribuidor())
                    .precioPublicoUnitario(calculo.precioPublico())
                    .descuentoUnitario(calculo.descuento())
                    .precioFinalUnitario(calculo.precioFinal())
                    .subtotalCliente(subtotalCliente)
                    .subtotalEmpresa(subtotalEmpresa)
                    .subtotalDescuento(subtotalDescuento)
                    .gananciaDistribuidor(ganancia)
                    .build());

            totalCliente = totalCliente.add(subtotalCliente);
            totalEmpresa = totalEmpresa.add(subtotalEmpresa);
            totalDescuento = totalDescuento.add(subtotalDescuento);
            totalGanancia = totalGanancia.add(ganancia);
        }

        compra.setTotalCliente(totalCliente);
        compra.setTotalEmpresa(totalEmpresa);
        compra.setTotalDescuento(totalDescuento);
        compra.setTotalGananciaDistribuidor(totalGanancia);
        return compraPublicaDao.save(compra);
    }

    @Override
    public List<CompraPublica> listarComprasPublicas() {
        return compraPublicaDao.findAllByOrderByFechaCompraDesc().stream()
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .toList();
    }

    @Override
    public List<ClientePublicoAdminResponse> listarClientesPublicos(Long distribuidorId, Long tipoClienteId) {
        List<ClientePublico> clientes = distribuidorId == null
                ? clientePublicoDao.findAllByOrderByFechaRegistroDesc()
                : clientePublicoDao.findByDistribuidorIdOrderByFechaRegistroDesc(distribuidorId);

        return clientes.stream()
                .filter(cliente -> Auditoria.ESTADO_ACTIVO.equals(cliente.getEstado()))
                .filter(cliente -> tipoClienteId == null || (
                        cliente.getTipoCliente() != null
                                && cliente.getTipoCliente().getId() != null
                                && cliente.getTipoCliente().getId().equals(tipoClienteId)
                ))
                .map(this::toClientePublicoAdminResponse)
                .toList();
    }

    @Override
    public Optional<ClientePublicoResponse> buscarClientePorDocumento(String documento) {
        String normalized = normalizarTexto(documento);
        if (normalized == null) {
            return Optional.empty();
        }

        Optional<ClientePublicoResponse> persona = usuarioDao.findAll().stream()
                .map(Usuario::getPersona)
                .filter(item -> item != null && Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .filter(item -> normalized.equalsIgnoreCase(normalizarTexto(item.getDocumento())))
                .findFirst()
                .map(item -> new ClientePublicoResponse(
                        item.getNombres(),
                        item.getApellidos(),
                        item.getDocumento(),
                        item.getEmail(),
                        item.getTelefono(),
                        Boolean.FALSE,
                        null,
                        null,
                        null,
                        "PERSONA"
                ));
        if (persona.isPresent()) {
            return persona;
        }

        return compraPublicaDao.findFirstByClienteDocumentoOrderByFechaCompraDesc(normalized)
                .filter(compra -> Auditoria.ESTADO_ACTIVO.equals(compra.getEstado()))
                .map(compra -> new ClientePublicoResponse(
                        compra.getClienteNombres(),
                        compra.getClienteApellidos(),
                        compra.getClienteDocumento(),
                        compra.getClienteEmail(),
                        compra.getClienteTelefono(),
                        compra.getEnvioRequiere(),
                        compra.getEnvioDireccion(),
                        compra.getEnvioCiudad(),
                        compra.getEnvioReferencia(),
                        "COMPRA_PUBLICA"
                ));
    }

    @Override
    public Optional<ClientePublicoResponse> buscarClientePorDocumento(String username, String documento) {
        String normalized = normalizarTexto(documento);
        if (normalized == null) {
            return Optional.empty();
        }

        Usuario usuario = buscarDistribuidor(username)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada."));
        Long distribuidorId = usuario.getPersona().getId();
        Optional<ClientePublicoResponse> clientePublico = clientePublicoDao
                .findByDistribuidorIdAndDocumentoIgnoreCase(distribuidorId, normalized)
                .filter(cliente -> Auditoria.ESTADO_ACTIVO.equals(cliente.getEstado()))
                .map(this::toClientePublicoResponse);
        return clientePublico.or(() -> buscarClientePorDocumento(documento));
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public CompraPublica cambiarEstado(Long compraId, String estadoCompra, String usuarioOperacion) {
        String estado = normalizarTexto(estadoCompra);
        if (estado == null || !ESTADOS_VALIDOS.contains(estado.toUpperCase())) {
            throw new IllegalArgumentException("Estado de compra publica no valido.");
        }

        CompraPublica compra = compraPublicaDao.findById(compraId)
                .filter(found -> Auditoria.ESTADO_ACTIVO.equals(found.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Compra publica no encontrada."));
        String anterior = compra.getEstadoCompra();
        String nuevo = estado.toUpperCase();
        compra.setEstadoCompra(nuevo);
        registrarAuditoriaEstado(compra, nuevo, usuarioOperacion);
        compra = compraPublicaDao.save(compra);

        if (!esEstadoPagado(anterior) && esEstadoPagado(nuevo)) {
            acreditarGananciaDistribuidor(compra);
        }

        return compra;
    }

    private ProductoPublicoResponse toProductoPublico(Producto producto, TipoClientePublico tipoCliente) {
        BigDecimal precioDistribuidor = zeroIfNull(producto.getPrecio());
        BigDecimal precioPublico = zeroIfNull(producto.getPrecioPublico());
        if (precioPublico.compareTo(BigDecimal.ZERO) <= 0) {
            precioPublico = precioDistribuidor;
        }

        BigDecimal descuento = productoDescuentoClienteDao.findByProductoIdAndTipoClienteId(producto.getId(), tipoCliente.getId())
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .map(ProductoDescuentoCliente::getDescuentoMonto)
                .map(this::zeroIfNull)
                .orElse(BigDecimal.ZERO);
        BigDecimal precioFinal = precioPublico.subtract(descuento).max(precioDistribuidor);
        BigDecimal descuentoAplicado = precioPublico.subtract(precioFinal).max(BigDecimal.ZERO);

        return new ProductoPublicoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria(),
                producto.getImagenUrl(),
                precioDistribuidor,
                precioPublico,
                descuentoAplicado,
                precioFinal,
                tipoCliente.getCodigo(),
                tipoCliente.getNombre()
        );
    }

    private TipoClientePublico resolverTipoCliente(String codigo) {
        String tipo = normalizarTexto(codigo);
        if (tipo == null) {
            tipo = TIPO_CLIENTE_NORMAL;
        }
        return tipoClientePublicoDao.findByCodigoIgnoreCase(tipo)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cliente no valido."));
    }

    private ClientePublico guardarClientePublico(Persona distribuidor, TipoClientePublico tipoCliente, CompraPublicaRequest request) {
        String documento = normalizarTexto(request.clienteDocumento());
        if (documento == null) {
            throw new IllegalArgumentException("Ingresa el documento del cliente.");
        }

        ClientePublico cliente = clientePublicoDao
                .findByDistribuidorIdAndDocumentoIgnoreCase(distribuidor.getId(), documento)
                .orElseGet(ClientePublico::new);
        cliente.setDistribuidor(distribuidor);
        cliente.setTipoCliente(tipoCliente);
        cliente.setNombres(normalizarTexto(request.clienteNombres()));
        cliente.setApellidos(normalizarTexto(request.clienteApellidos()));
        cliente.setDocumento(documento);
        cliente.setEmail(normalizarTexto(request.clienteEmail()));
        cliente.setTelefono(normalizarTexto(request.clienteTelefono()));
        cliente.setEnvioDireccion(normalizarTexto(request.envioDireccion()));
        cliente.setEnvioCiudad(normalizarTexto(request.envioCiudad()));
        cliente.setEnvioReferencia(normalizarTexto(request.envioReferencia()));
        cliente.setEstado(Auditoria.ESTADO_ACTIVO);
        return clientePublicoDao.save(cliente);
    }

    private ClientePublicoResponse toClientePublicoResponse(ClientePublico cliente) {
        return new ClientePublicoResponse(
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getDocumento(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getEnvioDireccion() != null || cliente.getEnvioCiudad() != null,
                cliente.getEnvioDireccion(),
                cliente.getEnvioCiudad(),
                cliente.getEnvioReferencia(),
                "CLIENTE_PUBLICO"
        );
    }

    private ClientePublicoAdminResponse toClientePublicoAdminResponse(ClientePublico cliente) {
        Persona distribuidor = cliente.getDistribuidor();
        TipoClientePublico tipoCliente = cliente.getTipoCliente();
        String distribuidorNombre = distribuidor == null
                ? null
                : (normalizarTexto(distribuidor.getNombres() + " " + distribuidor.getApellidos()));
        return new ClientePublicoAdminResponse(
                cliente.getId(),
                distribuidor == null ? null : distribuidor.getId(),
                distribuidorNombre,
                tipoCliente == null ? null : tipoCliente.getId(),
                tipoCliente == null ? null : tipoCliente.getCodigo(),
                tipoCliente == null ? null : tipoCliente.getNombre(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getDocumento(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getEnvioDireccion(),
                cliente.getEnvioCiudad(),
                cliente.getEnvioReferencia(),
                cliente.getFechaRegistro()
        );
    }

    private void validarCompra(CompraPublicaRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un producto.");
        }
        if (normalizarTexto(request.clienteNombres()) == null || normalizarTexto(request.clienteApellidos()) == null) {
            throw new IllegalArgumentException("Ingresa nombre y apellido del cliente.");
        }
        if (normalizarTexto(request.clienteDocumento()) == null) {
            throw new IllegalArgumentException("Ingresa el documento del cliente.");
        }
        if (Boolean.TRUE.equals(request.envioRequiere())
                && (normalizarTexto(request.envioDireccion()) == null || normalizarTexto(request.envioCiudad()) == null)) {
            throw new IllegalArgumentException("Ingresa direccion y ciudad de envio.");
        }
        String metodoPago = Optional.ofNullable(normalizarTexto(request.metodoPago())).orElse("");
        if (("TRANSFERENCIA".equalsIgnoreCase(metodoPago) || "QR".equalsIgnoreCase(metodoPago))
                && normalizarTexto(request.comprobantePagoUrl()) == null) {
            throw new IllegalArgumentException("Adjunta el comprobante de pago antes de registrar el pedido.");
        }
    }

    private void registrarAuditoriaEstado(CompraPublica compra, String estadoNuevo, String usuarioOperacion) {
        String operador = Optional.ofNullable(normalizarTexto(usuarioOperacion)).orElse("SISTEMA");
        LocalDateTime ahora = LocalDateTime.now();
        if (CompraPublica.ESTADO_VALIDADA.equals(estadoNuevo) && compra.getFechaValidacion() == null) {
            compra.setUsuarioValidacion(operador);
            compra.setFechaValidacion(ahora);
        }
        if (CompraPublica.ESTADO_ENTREGADA.equals(estadoNuevo)) {
            if (compra.getFechaValidacion() == null) {
                compra.setUsuarioValidacion(operador);
                compra.setFechaValidacion(ahora);
            }
            compra.setUsuarioEntrega(operador);
            compra.setFechaEntrega(ahora);
        }
    }

    private boolean esEstadoPagado(String estado) {
        return CompraPublica.ESTADO_VALIDADA.equals(estado) || CompraPublica.ESTADO_ENTREGADA.equals(estado);
    }

    private void acreditarGananciaDistribuidor(CompraPublica compra) {
        BigDecimal ganancia = zeroIfNull(compra.getTotalGananciaDistribuidor());
        if (ganancia.compareTo(BigDecimal.ZERO) <= 0
                || movimientoBilleteraDao.existsByReferenciaTipoAndReferenciaIdAndTipo(
                REFERENCIA_VENTA_PUBLICA,
                compra.getId(),
                MovimientoBilletera.TIPO_DINERO
        )) {
            return;
        }

        Billetera billetera = billeteraService.asegurarBilletera(compra.getDistribuidor());
        billetera.setSaldoDinero(zeroIfNull(billetera.getSaldoDinero()).add(ganancia));
        billetera = billeteraDao.save(billetera);
        movimientoBilleteraDao.save(MovimientoBilletera.builder()
                .billetera(billetera)
                .tipo(MovimientoBilletera.TIPO_DINERO)
                .concepto("Ganancia por venta publica #" + compra.getId())
                .referenciaTipo(REFERENCIA_VENTA_PUBLICA)
                .referenciaId(compra.getId())
                .monto(ganancia)
                .saldoResultado(billetera.getSaldoDinero())
                .build());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizarTexto(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
