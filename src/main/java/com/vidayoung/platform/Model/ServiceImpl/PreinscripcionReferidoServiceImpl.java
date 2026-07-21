package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.PreinscripcionReferidoDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Dao.RolDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PreinscripcionReferido;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.PersonaService;
import com.vidayoung.platform.Model.Service.PreinscripcionReferidoService;
import com.vidayoung.platform.Model.Service.ReferidoService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreinscripcionReferidoServiceImpl implements PreinscripcionReferidoService {

    private static final String DEFAULT_REFERIDO_ROLE = "DISTRIBUIDOR";

    private final PreinscripcionReferidoDao preinscripcionReferidoDao;
    private final PersonaDao personaDao;
    private final PlanDao planDao;
    private final ReferidoDao referidoDao;
    private final UsuarioDao usuarioDao;
    private final RolDao rolDao;
    private final PersonaService personaService;
    private final ReferidoService referidoService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PreinscripcionReferido> listar(String estadoPreinscripcion) {
        String estado = normalizar(estadoPreinscripcion);

        if (estado != null) {
            return preinscripcionReferidoDao.findByEstadoPreinscripcionOrderByFechaRegistroDesc(estado.toUpperCase()).stream()
                    .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                    .toList();
        }

        return preinscripcionReferidoDao.findAll().stream()
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .sorted(Comparator.comparing(PreinscripcionReferido::getFechaRegistro, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public PreinscripcionReferido crear(Long patrocinadorId, Long planId, String nombres, String apellidos, String documento, String telefono, String email, String username, String password, String confirmPassword) {
        Persona patrocinador = personaDao.findById(patrocinadorId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("La persona que refiere no existe."));
        Plan plan = planDao.findById(planId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("El plan seleccionado no existe."));
        String documentoNormalizado = requerido(documento, "El CI es obligatorio.");
        String usernameNormalizado = normalizarUsername(username);
        String passwordNormalizado = requerido(password, "La contrasena es obligatoria.");
        String confirmPasswordNormalizado = requerido(confirmPassword, "La confirmacion de contrasena es obligatoria.");

        if (!passwordNormalizado.equals(confirmPasswordNormalizado)) {
            throw new IllegalArgumentException("La confirmacion de contrasena no coincide.");
        }

        if (passwordNormalizado.length() < 6) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres.");
        }

        if (usuarioDao.existsByUsername(usernameNormalizado)) {
            throw new IllegalArgumentException("El nombre de usuario ya esta en uso.");
        }

        preinscripcionReferidoDao
                .findByDocumentoAndEstadoPreinscripcion(documentoNormalizado, PreinscripcionReferido.ESTADO_PREINSCRIPCION_PENDIENTE)
                .ifPresent(item -> {
                    throw new IllegalArgumentException("Ya existe una preinscripcion pendiente con ese CI.");
                });

        preinscripcionReferidoDao
                .findByUsernameSolicitadoAndEstadoPreinscripcion(usernameNormalizado, PreinscripcionReferido.ESTADO_PREINSCRIPCION_PENDIENTE)
                .ifPresent(item -> {
                    throw new IllegalArgumentException("Ya existe una preinscripcion pendiente con ese nombre de usuario.");
                });

        return preinscripcionReferidoDao.save(PreinscripcionReferido.builder()
                .patrocinador(patrocinador)
                .nombres(requerido(nombres, "Los nombres son obligatorios."))
                .apellidos(requerido(apellidos, "Los apellidos son obligatorios."))
                .documento(documentoNormalizado)
                .telefono(requerido(telefono, "El numero de celular es obligatorio."))
                .email(normalizar(email))
                .plan(plan)
                .usernameSolicitado(usernameNormalizado)
                .passwordSolicitado(passwordEncoder.encode(passwordNormalizado))
                .estadoPreinscripcion(PreinscripcionReferido.ESTADO_PREINSCRIPCION_PENDIENTE)
                .build());
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public PreinscripcionReferido validar(Long id, Long patrocinadorId, Long planId, String nombres, String apellidos, String documento, String telefono, String email, String usuarioValidacion) {
        PreinscripcionReferido preinscripcion = buscarActiva(id);
        Persona patrocinador = personaDao.findById(patrocinadorId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("La persona que refiere no existe."));
        Plan plan = planDao.findById(planId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("El plan seleccionado no existe."));
        String documentoNormalizado = requerido(documento, "El CI es obligatorio.");

        String nombresNormalizados = requerido(nombres, "Los nombres son obligatorios.");
        String apellidosNormalizados = requerido(apellidos, "Los apellidos son obligatorios.");
        String telefonoNormalizado = requerido(telefono, "El numero de celular es obligatorio.");
        Persona persona = personaDao.findByDocumento(documentoNormalizado)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseGet(Persona::new);

        persona.setNombres(nombresNormalizados);
        persona.setApellidos(apellidosNormalizados);
        persona.setDocumento(documentoNormalizado);
        persona.setTelefono(telefonoNormalizado);
        persona.setEmail(normalizar(email));
        persona = personaService.guardar(persona);

        if (referidoDao.existsByPersonaId(persona.getId())) {
            throw new IllegalArgumentException("La persona ya esta registrada en la red.");
        }

        Referido referido = referidoService.guardar(persona.getId(), patrocinador.getId(), plan.getId());
        crearOActualizarUsuario(preinscripcion, persona);

        preinscripcion.setPatrocinador(patrocinador);
        preinscripcion.setPlan(plan);
        preinscripcion.setPersona(persona);
        preinscripcion.setReferido(referido);
        preinscripcion.setNombres(persona.getNombres());
        preinscripcion.setApellidos(persona.getApellidos());
        preinscripcion.setDocumento(persona.getDocumento());
        preinscripcion.setTelefono(persona.getTelefono());
        preinscripcion.setEmail(persona.getEmail());
        preinscripcion.setEstadoPreinscripcion(PreinscripcionReferido.ESTADO_PREINSCRIPCION_VALIDADA);
        preinscripcion.setFechaValidacion(LocalDateTime.now());
        preinscripcion.setUsuarioValidacion(normalizar(usuarioValidacion) == null ? "SISTEMA" : normalizar(usuarioValidacion));

        return preinscripcionReferidoDao.save(preinscripcion);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public PreinscripcionReferido rechazar(Long id, String observacion, String usuarioValidacion) {
        PreinscripcionReferido preinscripcion = buscarActiva(id);
        preinscripcion.setEstadoPreinscripcion(PreinscripcionReferido.ESTADO_PREINSCRIPCION_RECHAZADA);
        preinscripcion.setObservacion(normalizar(observacion));
        preinscripcion.setFechaValidacion(LocalDateTime.now());
        preinscripcion.setUsuarioValidacion(normalizar(usuarioValidacion) == null ? "SISTEMA" : normalizar(usuarioValidacion));
        return preinscripcionReferidoDao.save(preinscripcion);
    }

    private PreinscripcionReferido buscarActiva(Long id) {
        return preinscripcionReferidoDao.findById(id)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Preinscripcion no encontrada."));
    }

    private String requerido(String value, String message) {
        String normalized = normalizar(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizarUsername(String value) {
        String normalized = requerido(value, "El nombre de usuario es obligatorio.").toLowerCase();

        if (!normalized.matches("^[a-z0-9._-]{4,50}$")) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 4 y 50 caracteres y solo puede usar letras, numeros, punto, guion o guion bajo.");
        }

        return normalized;
    }

    private void crearOActualizarUsuario(PreinscripcionReferido preinscripcion, Persona persona) {
        if (normalizar(preinscripcion.getUsernameSolicitado()) == null
                || normalizar(preinscripcion.getPasswordSolicitado()) == null) {
            return;
        }

        String username = normalizarUsername(preinscripcion.getUsernameSolicitado());

        usuarioDao.findByUsername(username)
                .filter(usuario -> !usuario.getPersona().getId().equals(persona.getId()))
                .ifPresent(usuario -> {
                    throw new IllegalArgumentException("El nombre de usuario ya esta en uso.");
                });

        Usuario usuario = usuarioDao.findByPersonaId(persona.getId()).orElseGet(Usuario::new);
        usuario.setUsername(username);
        usuario.setPassword(preinscripcion.getPasswordSolicitado());
        usuario.setActivo(true);
        usuario.setPersona(persona);
        usuario.setEstado(Auditoria.ESTADO_ACTIVO);

        HashSet<Rol> roles = new HashSet<>(usuario.getRoles() == null ? List.of() : usuario.getRoles());
        roles.add(rolDao.findByNombre(DEFAULT_REFERIDO_ROLE)
                .orElseThrow(() -> new IllegalArgumentException("El rol DISTRIBUIDOR no existe.")));
        usuario.setRoles(roles);

        usuarioDao.save(usuario);
    }

    private String normalizar(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
