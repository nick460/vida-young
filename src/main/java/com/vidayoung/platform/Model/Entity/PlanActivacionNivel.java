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
        name = "planes_activacion_niveles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_planes_activacion_nivel",
                columnNames = {"plan_activacion_id", "numero_nivel"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PlanActivacionNivel extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_activacion_id", nullable = false)
    @JsonIgnoreProperties({"niveles"})
    @ToString.Exclude
    private PlanActivacion planActivacion;

    @Column(name = "numero_nivel", nullable = false)
    private Integer numeroNivel;

    @Column(name = "monto_por_producto", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoPorProducto = BigDecimal.ZERO;
}
