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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Notificacion extends Auditoria {

    public static final String TIPO_INFO = "INFO";
    public static final String TIPO_COMPRA = "COMPRA";
    public static final String TIPO_MEMBRESIA = "MEMBRESIA";
    public static final String TIPO_RECOMPENSA = "RECOMPENSA";
    public static final String TIPO_RANGO = "RANGO";
    public static final String TIPO_SISTEMA = "SISTEMA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id")
    @JsonIgnoreProperties({"usuario", "rangoActual", "hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private Persona destinatario;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String tipo = TIPO_INFO;

    @Column(length = 255)
    private String link;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "fecha_leida")
    private LocalDateTime fechaLeida;

    @Column(name = "fecha_enviado", nullable = false)
    private LocalDateTime fechaEnviado;
}
