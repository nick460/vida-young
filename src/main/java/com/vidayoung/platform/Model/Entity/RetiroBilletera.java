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
@Table(name = "retiros_billetera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RetiroBilletera extends Auditoria {

    public static final String ESTADO_PROCESADO = "PROCESADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona persona;

    @Column(name = "monto_dinero", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoDinero = BigDecimal.ZERO;

    @Column(name = "monto_productos", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoProductos = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String estadoRetiro;

    @Column(name = "fecha_retiro", nullable = false)
    private LocalDateTime fechaRetiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    @JsonIgnoreProperties({"gestion"})
    @ToString.Exclude
    private PeriodoGestion periodo;

    @Column(length = 240)
    private String observacion;

    @jakarta.persistence.OneToMany(mappedBy = "retiro", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"retiro"})
    @ToString.Exclude
    @Builder.Default
    private List<RetiroBilleteraDetalle> detalles = new ArrayList<>();
}
