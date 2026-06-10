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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "recompensas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Recompensa extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referido_id", nullable = false)
    @JsonIgnoreProperties({"patrocinador"})
    @ToString.Exclude
    private Referido referido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona beneficiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_ingreso_id", nullable = false)
    @JsonIgnoreProperties({"niveles", "productos"})
    @ToString.Exclude
    private Plan planIngreso;

    @Column(name = "nivel_generado", nullable = false)
    private Integer nivelGenerado;

    @Column(name = "monto_efectivo", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoEfectivo = BigDecimal.ZERO;

    @Column(name = "valor_productos", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorProductos = BigDecimal.ZERO;
}
