package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Config.FirebaseConfig;
import com.vidayoung.platform.Model.Dao.DispositivoDao;
import com.vidayoung.platform.Model.Dao.NotificacionDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Dispositivo;
import com.vidayoung.platform.Model.Entity.Notificacion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Service.NotificacionService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private static final String DESTINO_USUARIO = "/queue/notificaciones";
    private static final String DESTINO_BROADCAST = "/topic/notificaciones";

    private final NotificacionDao notificacionDao;
    private final PersonaDao personaDao;
    private final UsuarioDao usuarioDao;
    private final SimpMessagingTemplate messagingTemplate;
    private final DispositivoDao dispositivoDao;
    private final FirebaseConfig firebaseConfig;

    @Override
    @Transactional
    public Notificacion notificarPersona(Long personaId, String tipo, String titulo, String mensaje, String link) {
        if (personaId == null) {
            return null;
        }

        Persona persona = personaDao.findById(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElse(null);
        if (persona == null) {
            return null;
        }

        Notificacion notificacion = guardar(persona, tipo, titulo, mensaje, link);

        // Enviar via WebSocket al usuario
        Optional<String> username = usuarioDao.findByPersonaId(personaId).map(usuario -> usuario.getUsername());
        username.ifPresent(value -> messagingTemplate.convertAndSendToUser(
                value,
                DESTINO_USUARIO,
                NotificacionPush.desde(notificacion)
        ));

        // Enviar via FCM a todos los dispositivos activos de la persona
        if (firebaseConfig.isEnabled()) {
            enviarFCMAPersona(personaId, titulo, mensaje, link, tipo);
        }

        return notificacion;
    }

    @Override
    @Transactional
    public Notificacion notificarBroadcast(String tipo, String titulo, String mensaje, String link) {
        Notificacion notificacion = guardar(null, tipo, titulo, mensaje, link);
        messagingTemplate.convertAndSend(DESTINO_BROADCAST, NotificacionPush.desde(notificacion));

        // Enviar via FCM a todos los dispositivos activos
        if (firebaseConfig.isEnabled()) {
            try {
                List<String> tokens = dispositivoDao.findByActivoTrue().stream()
                        .map(Dispositivo::getToken)
                        .toList();
                if (tokens.isEmpty()) {
                    System.out.println("⚠️ No hay dispositivos activos para broadcast FCM");
                } else {
                    enviarFCMATokens(tokens, titulo, mensaje, link, tipo);
                }
            } catch (Exception e) {
                System.err.println("❌ Error enviando FCM broadcast: " + e.getMessage());
            }
        }

        return notificacion;
    }

    private void enviarFCMAPersona(Long personaId, String titulo, String mensaje, String link, String tipo) {
        try {
            List<Dispositivo> dispositivos = dispositivoDao.findByPersonaIdAndActivoTrue(personaId);
            if (dispositivos.isEmpty()) {
                System.out.println("⚠️ No hay dispositivos activos para persona ID: " + personaId);
                return;
            }

            List<String> tokens = dispositivos.stream()
                    .map(Dispositivo::getToken)
                    .toList();

            enviarFCMATokens(tokens, titulo, mensaje, link, tipo);
        } catch (Exception e) {
            System.err.println("❌ Error enviando FCM a persona " + personaId + ": " + e.getMessage());
        }
    }

    private void enviarFCMATokens(List<String> tokens, String titulo, String mensaje, String link, String tipo) {
        int batch = 50;
        for (int i = 0; i < tokens.size(); i += batch) {
            for (String token : tokens.subList(i, Math.min(i + batch, tokens.size()))) {
                try {
                    // Solo payload data: el service worker construye la notificacion
                    // (evita la notificacion duplicada automatica de FCM)
                    Message message = Message.builder()
                            .setToken(token)
                            .putData("titulo", titulo != null ? titulo : "Nueva notificacion")
                            .putData("mensaje", mensaje != null ? mensaje : "")
                            .putData("link", link)
                            .putData("tipo", tipo != null ? tipo : "INFO")
                            .build();

                    FirebaseMessaging.getInstance().send(message);
                    System.out.println("✅ FCM enviado a token " + token.substring(0, Math.min(20, token.length())) + "...");
                } catch (Exception e) {
                    System.err.println("❌ FCM falló para token "
                            + token.substring(0, Math.min(20, token.length())) + "...: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public List<Notificacion> listarMias(Long personaId) {
        return notificacionDao.findMias(personaId, Auditoria.ESTADO_ACTIVO);
    }

    @Override
    public long contarNoLeidas(Long personaId) {
        return notificacionDao.countMiasNoLeidas(personaId, Auditoria.ESTADO_ACTIVO);
    }

    @Override
    @Transactional
    public Notificacion marcarLeida(Long notificacionId, Long personaId) {
        Notificacion notificacion = notificacionDao.findById(notificacionId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada."));

        if (notificacion.getDestinatario() != null
                && !notificacion.getDestinatario().getId().equals(personaId)) {
            throw new IllegalArgumentException("No tiene permiso para modificar esta notificacion.");
        }

        if (Boolean.FALSE.equals(notificacion.getLeida())) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacion = notificacionDao.save(notificacion);
        }
        return notificacion;
    }

    @Override
    @Transactional
    public long marcarTodasLeidas(Long personaId) {
        List<Notificacion> noLeidas = notificacionDao.findMias(personaId, Auditoria.ESTADO_ACTIVO).stream()
                .filter(notificacion -> Boolean.FALSE.equals(notificacion.getLeida()))
                .toList();
        LocalDateTime ahora = LocalDateTime.now();
        noLeidas.forEach(notificacion -> {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(ahora);
            notificacionDao.save(notificacion);
        });
        return noLeidas.size();
    }

    private Notificacion guardar(Persona destinatario, String tipo, String titulo, String mensaje, String link) {
        return notificacionDao.save(Notificacion.builder()
                .destinatario(destinatario)
                .tipo(normalizarTexto(tipo) == null ? Notificacion.TIPO_INFO : normalizarTexto(tipo))
                .titulo(normalizarTexto(titulo))
                .mensaje(normalizarTexto(mensaje))
                .link(normalizarTexto(link))
                .leida(false)
                .fechaEnviado(LocalDateTime.now())
                .build());
    }

    private String normalizarTexto(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}