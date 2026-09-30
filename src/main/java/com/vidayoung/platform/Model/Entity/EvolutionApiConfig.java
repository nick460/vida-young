package com.vidayoung.platform.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evolution_api_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EvolutionApiConfig extends Auditoria {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private Boolean habilitado = false;

    @Column(name = "api_url", length = 255)
    private String apiUrl;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "instance_name", length = 120)
    private String instanceName;

    @Column(name = "codigo_pais", length = 8)
    @Builder.Default
    private String codigoPais = "591";

    @Column(name = "login_url", length = 255)
    @Builder.Default
    private String loginUrl = "https://vidayoung.online/login";

    @Column(name = "ultimo_template")
    @Builder.Default
    private Integer ultimoTemplate = -1;
}
