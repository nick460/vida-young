package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Dto.PresenciaResponse;
import com.vidayoung.platform.Model.Dao.PresenciaUsuarioDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.PresenciaUsuario;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.PresenciaService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenciaServiceImpl implements PresenciaService {

    private final PresenciaUsuarioDao presenciaDao;
    private final UsuarioDao usuarioDao;

    @Override
    @Transactional
    public void registrarLatido(String username, String ip, String userAgent, String rutaActual) {
        if (username == null || username.isBlank()) {
            return;
        }
        Usuario usuario = usuarioDao.findByUsername(username)
                .filter(u -> Auditoria.ESTADO_ACTIVO.equals(u.getEstado()))
                .orElse(null);
        if (usuario == null || usuario.getPersona() == null) {
            return;
        }
        Long personaId = usuario.getPersona().getId();

        PresenciaUsuario presencia = presenciaDao.findByPersonaId(personaId)
                .orElse(PresenciaUsuario.builder().persona(usuario.getPersona()).build());
        presencia.setPersona(usuario.getPersona());
        presencia.setUsername(usuario.getUsername());
        presencia.setUltimoLatido(LocalDateTime.now());
        presencia.setIp(truncar(ip, 45));
        presencia.setUserAgent(truncar(userAgent, 300));
        presencia.setRutaActual(truncar(rutaActual, 200));
        presencia.setEstado(Auditoria.ESTADO_ACTIVO);
        presenciaDao.save(presencia);
    }

    @Override
    @Transactional
    public void marcarSalida(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        usuarioDao.findByUsername(username).ifPresent(usuario -> {
            if (usuario.getPersona() != null) {
                presenciaDao.findByPersonaId(usuario.getPersona().getId())
                        .ifPresent(presenciaDao::delete);
            }
        });
    }

    @Override
    public List<PresenciaResponse> listarEnLinea(int minutos) {
        int ventana = minutos < 1 ? 2 : minutos;
        LocalDateTime desde = LocalDateTime.now().minusMinutes(ventana);
        LocalDateTime ahora = LocalDateTime.now();

        return presenciaDao.findByUltimoLatidoAfterOrderByUltimoLatidoDesc(desde).stream()
                .filter(p -> Auditoria.ESTADO_ACTIVO.equals(p.getEstado()))
                .filter(p -> p.getPersona() != null)
                .map(p -> {
                    Usuario usuario = usuarioDao.findByUsername(p.getUsername()).orElse(null);
                    List<String> roles = usuario == null || usuario.getRoles() == null
                            ? List.of()
                            : usuario.getRoles().stream().map(r -> r.getNombre()).toList();
                    long segundos = ChronoUnit.SECONDS.between(p.getUltimoLatido(), ahora);
                    return new PresenciaResponse(
                            p.getPersona().getId(),
                            p.getPersona().getNombres(),
                            p.getPersona().getApellidos(),
                            p.getUsername(),
                            roles,
                            p.getUltimoLatido(),
                            Math.max(0, segundos),
                            p.getIp(),
                            p.getRutaActual()
                    );
                })
                .toList();
    }

    @Override
    public long contarEnLinea(int minutos) {
        return listarEnLinea(minutos).size();
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
