package com.vidayoung.platform.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menus_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MenuSistema extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "menu_id", nullable = false, unique = true, length = 80)
    private String menuId;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 80)
    private String icon;

    @Column(nullable = false)
    @Builder.Default
    private Boolean custom = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}
