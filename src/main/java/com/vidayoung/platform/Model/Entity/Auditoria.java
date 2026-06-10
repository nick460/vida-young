package com.vidayoung.platform.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@MappedSuperclass
@Getter
@Setter
public abstract class Auditoria {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_ELIMINADO = "ELIMINADO";

    @Column(name = "usuario_registro", nullable = false, updatable = false, length = 50)
    private String usuarioRegistro;

    @Column(name = "usuario_modificacion", length = 50)
    private String usuarioModificacion;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(nullable = false, length = 30)
    private String estado = ESTADO_ACTIVO;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        String currentUser = currentUsername();

        fechaRegistro = now;
        fechaModificacion = now;
        usuarioRegistro = currentUser;
        usuarioModificacion = currentUser;

        if (estado == null || estado.isBlank()) {
            estado = ESTADO_ACTIVO;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        fechaModificacion = LocalDateTime.now();
        usuarioModificacion = currentUsername();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }

        String username = authentication.getName();
        return username == null || username.isBlank() ? "SYSTEM" : username;
    }
}
