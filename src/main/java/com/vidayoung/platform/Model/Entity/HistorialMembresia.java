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
import jakarta.persistence.Transient;
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
@Table(name = "historial_membresias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class HistorialMembresia extends Auditoria {

    public static final String TIPO_AFILIACION = "AFILIACION";
    public static final String TIPO_ACTIVACION = "ACTIVACION";
    public static final String MEMBRESIA_ACTIVA = "ACTIVA";
    public static final String MEMBRESIA_VENCIDA = "VENCIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnoreProperties({"niveles", "productos"})
    @ToString.Exclude
    private Plan plan;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "precio_plan", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal precioPlan = BigDecimal.ZERO;

    @Column(name = "qp_plan", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal qpPlan = BigDecimal.ZERO;

    @Column(name = "referencia_tipo", length = 60)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "estado_membresia", nullable = false, length = 30)
    private String estadoMembresia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    @JsonIgnoreProperties({"gestion"})
    @ToString.Exclude
    private PeriodoGestion periodo;

    @Transient
    private String nombreActivacion;
}
