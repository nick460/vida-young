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
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Compra extends Auditoria {

    public static final String ESTADO_COMPRA_CONFIRMADA = "CONFIRMADA";
    public static final String ESTADO_COMPRA_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_COMPRA_VALIDADA = "VALIDADA";
    public static final String ESTADO_COMPRA_ENTREGADA = "ENTREGADA";
    public static final String ESTADO_COMPRA_RECHAZADA = "RECHAZADA";
    public static final String METODO_PAGO_TRANSFERENCIA = "TRANSFERENCIA";
    public static final String METODO_PAGO_QR = "QR";
    public static final String METODO_PAGO_CAJA = "CAJA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona persona;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_pv", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalPv = BigDecimal.ZERO;

    @Column(name = "total_qp", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalQp = BigDecimal.ZERO;

    @Column(name = "total_cr", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCr = BigDecimal.ZERO;

    @Column(name = "estado_compra", nullable = false, length = 30)
    private String estadoCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    @JsonIgnoreProperties({"gestion"})
    @ToString.Exclude
    private PeriodoGestion periodo;

    @Column(name = "usuario_validacion", length = 80)
    private String usuarioValidacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "usuario_entrega", length = 80)
    private String usuarioEntrega;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(name = "banco_pago", length = 120)
    private String bancoPago;

    @Column(name = "cuenta_pago", length = 80)
    private String cuentaPago;

    @Column(name = "codigo_pago", length = 30)
    private String codigoPago;

    @Column(name = "referencia_pago", length = 180)
    private String referenciaPago;

    @Column(name = "comprobante_pago_url", length = 255)
    private String comprobantePagoUrl;

    @Column(name = "comprobante_pago_nombre", length = 180)
    private String comprobantePagoNombre;

    @Column(name = "comprobante_pago_tipo", length = 80)
    private String comprobantePagoTipo;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("compra")
    @ToString.Exclude
    @Builder.Default
    private List<CompraDetalle> detalles = new ArrayList<>();
}
