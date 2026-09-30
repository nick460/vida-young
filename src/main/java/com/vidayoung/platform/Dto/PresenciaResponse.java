package com.vidayoung.platform.Dto;

import java.time.LocalDateTime;
import java.util.List;

public record PresenciaResponse(
        Long personaId,
        String nombres,
        String apellidos,
        String username,
        List<String> roles,
        LocalDateTime ultimoLatido,
        long segundosInactivo,
        String ip,
        String rutaActual
) {
}
