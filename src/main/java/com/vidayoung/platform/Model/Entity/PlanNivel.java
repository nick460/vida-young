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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "planes_niveles",
        uniqueConstraints = @UniqueConstraint(name = "uk_planes_niveles_plan_nivel", columnNames = {"plan_id", "numero_nivel"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PlanNivel extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnoreProperties({"niveles", "productos"})
    @ToString.Exclude
    private Plan plan;

    @Column(name = "numero_nivel", nullable = false)
    private Integer numeroNivel;

    @Column(name = "porcentaje_comision", nullable = false, precision = 12, scale = 2)
    private BigDecimal porcentajeComision;

    @Column(name = "valor_productos_beneficio", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorProductosBeneficio = BigDecimal.ZERO;
}
