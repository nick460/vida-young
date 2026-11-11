package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Dto.HistorialAccesoResponse;
import com.vidayoung.platform.Dto.PresenciaResponse;
import com.vidayoung.platform.Model.Dao.HistorialAccesoDao;
import com.vidayoung.platform.Model.Dao.PresenciaUsuarioDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.HistorialAcceso;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final HistorialAccesoDao historialDao;
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

        presenciaDao.registrarLatido(
                personaId,
                usuario.getUsername(),
                truncar(ip, 45),
                truncar(userAgent, 300),
                truncar(rutaActual, 200)
        );
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
                historialDao.findFirstByPersonaIdAndFechaSalidaIsNullOrderByFechaIngresoDesc(usuario.getPersona().getId())
                        .ifPresent(acceso -> {
                            acceso.setFechaSalida(LocalDateTime.now());
                            historialDao.save(acceso);
                        });
            }
        });
    }

    @Override
    @Transactional
    public void registrarIngreso(String username, String ip, String userAgent, String rutaInicial) {
        if (username == null || username.isBlank()) {
            return;
        }
        Usuario usuario = usuarioDao.findByUsername(username)
                .filter(u -> Auditoria.ESTADO_ACTIVO.equals(u.getEstado()))
                .orElse(null);
        if (usuario == null || usuario.getPersona() == null) {
            return;
        }
        HistorialAcceso acceso = HistorialAcceso.builder()
                .persona(usuario.getPersona())
                .username(usuario.getUsername())
                .fechaIngreso(LocalDateTime.now())
                .ip(truncar(ip, 45))
                .userAgent(truncar(userAgent, 300))
                .rutaInicial(truncar(rutaInicial, 200))
                .build();
        historialDao.save(acceso);
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

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<HistorialAccesoResponse> listarHistorial(int limite) {
        int size = limite < 1 ? 100 : Math.min(limite, 500);
        return historialDao.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "fechaIngreso"))).stream()
                .filter(a -> Auditoria.ESTADO_ACTIVO.equals(a.getEstado()))
                .filter(a -> a.getPersona() != null)
                .map(a -> {
                    Usuario usuario = a.getUsername() == null ? null : usuarioDao.findByUsername(a.getUsername()).orElse(null);
                    List<String> roles = usuario == null || usuario.getRoles() == null
                            ? List.of()
                            : usuario.getRoles().stream().map(r -> r.getNombre()).toList();
                    return new HistorialAccesoResponse(
                            a.getId(),
                            a.getPersona().getId(),
                            a.getPersona().getNombres(),
                            a.getPersona().getApellidos(),
                            a.getUsername(),
                            roles,
                            a.getFechaIngreso(),
                            a.getFechaSalida(),
                            a.getIp(),
                            a.getUserAgent(),
                            a.getRutaInicial()
                    );
                })
                .toList();
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
