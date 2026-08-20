package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Notificacion;
import java.util.List;

public interface NotificacionService {

    Notificacion notificarPersona(Long personaId, String tipo, String titulo, String mensaje, String link);

    Notificacion notificarBroadcast(String tipo, String titulo, String mensaje, String link);

    List<Notificacion> listarMias(Long personaId);

    long contarNoLeidas(Long personaId);

    Notificacion marcarLeida(Long notificacionId, Long personaId);

    long marcarTodasLeidas(Long personaId);

    record NotificacionPush(
            Long id,
            String tipo,
            String titulo,
            String mensaje,
            String link,
            Boolean leida,
            java.time.LocalDateTime fechaEnviado
    ) {
        public static NotificacionPush desde(Notificacion notificacion) {
            return new NotificacionPush(
                    notificacion.getId(),
                    notificacion.getTipo(),
                    notificacion.getTitulo(),
                    notificacion.getMensaje(),
                    notificacion.getLink(),
                    notificacion.getLeida(),
                    notificacion.getFechaEnviado()
            );
        }
    }
}
