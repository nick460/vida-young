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
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.Message;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        return notificacion;
    }

    private void enviarFCMAPersona(Long personaId, String titulo, String mensaje, String link, String tipo) {
        try {
            List<Dispositivo> dispositivos = dispositivoDao.findByPersonaIdAndActivoTrue(personaId);
            if (dispositivos.isEmpty()) {
                System.out.println("⚠️ No hay dispositivos activos para persona ID: " + personaId);
                return;
            }

            // Enviar en batches de 50 tokens (límite de FCM)
            List<String> tokens = dispositivos.stream()
                    .map(Dispositivo::getToken)
                    .toList();

            int batchSize = 50;
            for (int i = 0; i < tokens.size(); i += batchSize) {
                List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
                String[] tokenArray = batch.toArray(new String[0]);

                // Construir mensaje individual por token para evitar límite de multicast
                for (String token : tokenArray) {
                    Notification notification = Notification.builder()
                            .setTitle(titulo)
                            .setBody(mensaje)
                            .build();

                    Message message = Message.builder()
                            .setToken(token)
                            .setNotification(notification)
                            .putData("link", link)
                            .putData("tipo", tipo != null ? tipo : "INFO")
                            .build();

                    String response = com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
                    System.out.println("✅ FCM enviado a token " + token.substring(0, Math.min(20, token.length())) + "...");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error enviando FCM a persona " + personaId + ": " + e.getMessage());
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