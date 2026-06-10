package com.vidayoung.platform.Model.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "planes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Plan extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal qp = BigDecimal.ZERO;

    @Column(name = "bonificacion_directa", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bonificacionDirecta = BigDecimal.ZERO;

    @Column(name = "valor_productos_beneficio", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorProductosBeneficio = BigDecimal.ZERO;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "niveles_alcance", nullable = false)
    private Integer nivelesAlcance;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("plan")
    @ToString.Exclude
    @Builder.Default
    private Set<PlanNivel> niveles = new HashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("plan")
    @ToString.Exclude
    @Builder.Default
    private Set<PlanProducto> productos = new HashSet<>();
}
