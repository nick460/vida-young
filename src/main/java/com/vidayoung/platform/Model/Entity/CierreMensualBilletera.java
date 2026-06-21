package com.vidayoung.platform.Model.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "cierres_mensuales_billetera",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cierres_mensuales_persona_periodo",
                columnNames = {"persona_id", "periodo"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CierreMensualBilletera extends Auditoria {

    public static final String ESTADO_PLANILLA_PENDIENTE = "PENDIENTE_PLANILLA";
    public static final String ESTADO_PLANILLA_PAGADA = "PAGADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona persona;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(name = "saldo_dinero", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoDinero = BigDecimal.ZERO;

    @Column(name = "saldo_pv", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoPv = BigDecimal.ZERO;

    @Column(name = "saldo_qp", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoQp = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rango_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private Rango rango;

    @Column(name = "rango_nombre", length = 100)
    private String rangoNombre;

    @Column(name = "rango_qp_minimo", precision = 12, scale = 2)
    private BigDecimal rangoQpMinimo;

    @Column(name = "estado_planilla", nullable = false, length = 30)
    private String estadoPlanilla;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;
}
