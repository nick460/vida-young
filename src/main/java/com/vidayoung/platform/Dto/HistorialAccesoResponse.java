package com.vidayoung.platform.Dto;

import java.time.LocalDateTime;
import java.util.List;

public record HistorialAccesoResponse(
        Long id,
        Long personaId,
        String nombres,
        String apellidos,
        String username,
        List<String> roles,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaSalida,
        String ip,
        String userAgent,
        String rutaInicial
) {
}
