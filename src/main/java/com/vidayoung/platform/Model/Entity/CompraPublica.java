package com.vidayoung.platform.Model.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "compras_publicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CompraPublica extends Auditoria {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_VALIDADA = "VALIDADA";
    public static final String ESTADO_ENTREGADA = "ENTREGADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribuidor_id", nullable = false)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona distribuidor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private TipoClientePublico tipoCliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_publico_id", nullable = false)
    @JsonIgnoreProperties({"distribuidor", "tipoCliente"})
    @ToString.Exclude
    private ClientePublico clientePublico;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(name = "estado_compra", nullable = false, length = 30)
    private String estadoCompra;

    @Column(name = "cliente_nombres", nullable = false, length = 100)
    private String clienteNombres;

    @Column(name = "cliente_apellidos", nullable = false, length = 100)
    private String clienteApellidos;

    @Column(name = "cliente_documento", nullable = false, length = 40)
    private String clienteDocumento;

    @Column(name = "cliente_email", length = 120)
    private String clienteEmail;

    @Column(name = "cliente_telefono", length = 40)
    private String clienteTelefono;

    @Column(name = "envio_requiere", nullable = false)
    @Builder.Default
    private Boolean envioRequiere = Boolean.FALSE;

    @Column(name = "envio_direccion", length = 220)
    private String envioDireccion;

    @Column(name = "envio_ciudad", length = 80)
    private String envioCiudad;

    @Column(name = "envio_referencia", length = 220)
    private String envioReferencia;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(name = "referencia_pago", length = 180)
    private String referenciaPago;

    @Column(name = "comprobante_pago_url", length = 255)
    private String comprobantePagoUrl;

    @Column(name = "comprobante_pago_nombre", length = 180)
    private String comprobantePagoNombre;

    @Column(name = "comprobante_pago_tipo", length = 80)
    private String comprobantePagoTipo;

    @Column(name = "total_cliente", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCliente = BigDecimal.ZERO;

    @Column(name = "total_empresa", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEmpresa = BigDecimal.ZERO;

    @Column(name = "total_descuento", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(name = "total_ganancia_distribuidor", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGananciaDistribuidor = BigDecimal.ZERO;

    @Column(name = "usuario_validacion", length = 80)
    private String usuarioValidacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "usuario_entrega", length = 80)
    private String usuarioEntrega;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("compra")
    @ToString.Exclude
    @Builder.Default
    private List<CompraPublicaDetalle> detalles = new ArrayList<>();
}
