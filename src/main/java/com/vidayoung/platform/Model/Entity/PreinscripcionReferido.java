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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "preinscripciones_referidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PreinscripcionReferido extends Auditoria {

    public static final String ESTADO_PREINSCRIPCION_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_PREINSCRIPCION_VALIDADA = "VALIDADA";
    public static final String ESTADO_PREINSCRIPCION_RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patrocinador_id", nullable = false)
    @JsonIgnoreProperties({"usuario", "rangoActual"})
    @ToString.Exclude
    private Persona patrocinador;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 30)
    private String documento;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(name = "estado_preinscripcion", nullable = false, length = 30)
    @Builder.Default
    private String estadoPreinscripcion = ESTADO_PREINSCRIPCION_PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @JsonIgnoreProperties({"niveles", "productos"})
    @ToString.Exclude
    private Plan plan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    @JsonIgnoreProperties({"usuario", "rangoActual"})
    @ToString.Exclude
    private Persona persona;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referido_id")
    @JsonIgnoreProperties({"persona", "patrocinador", "plan"})
    @ToString.Exclude
    private Referido referido;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "usuario_validacion", length = 80)
    private String usuarioValidacion;

    @Column(length = 300)
    private String observacion;
}
