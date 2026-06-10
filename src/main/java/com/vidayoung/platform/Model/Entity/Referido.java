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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "referidos",
        uniqueConstraints = @UniqueConstraint(name = "uk_referidos_persona", columnNames = "persona_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Referido extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patrocinador_id")
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona patrocinador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnoreProperties({"niveles", "productos"})
    @ToString.Exclude
    private Plan plan;

    @Column(name = "fecha_union", nullable = false)
    private LocalDateTime fechaUnion;

    @Column(name = "fecha_inicio_membresia", nullable = false)
    private LocalDateTime fechaInicioMembresia;

    @Column(name = "fecha_fin_membresia", nullable = false)
    private LocalDateTime fechaFinMembresia;

    @Column(name = "membresia_activa", nullable = false)
    @Builder.Default
    private Boolean membresiaActiva = true;
}
