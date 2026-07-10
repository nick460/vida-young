package com.vidayoung.platform.Model.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        name = "billeteras",
        uniqueConstraints = @UniqueConstraint(name = "uk_billeteras_persona", columnNames = "persona_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Billetera extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"usuario"})
    @ToString.Exclude
    private Persona persona;

    @Column(name = "saldo_dinero", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoDinero = BigDecimal.ZERO;

    @Column(name = "saldo_pv", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoPv = BigDecimal.ZERO;

    @Column(name = "saldo_qp", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoQp = BigDecimal.ZERO;

    @Column(name = "saldo_cr", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoCr = BigDecimal.ZERO;
}
