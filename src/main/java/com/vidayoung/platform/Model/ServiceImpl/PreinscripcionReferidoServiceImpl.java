package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.PreinscripcionReferidoDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PreinscripcionReferido;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.PersonaService;
import com.vidayoung.platform.Model.Service.PreinscripcionReferidoService;
import com.vidayoung.platform.Model.Service.ReferidoService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreinscripcionReferidoServiceImpl implements PreinscripcionReferidoService {

    private final PreinscripcionReferidoDao preinscripcionReferidoDao;
    private final PersonaDao personaDao;
    private final PlanDao planDao;
    private final ReferidoDao referidoDao;
    private final PersonaService personaService;
    private final ReferidoService referidoService;

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
    public PreinscripcionReferido crear(Long patrocinadorId, String nombres, String apellidos, String documento, String telefono, String email) {
        Persona patrocinador = personaDao.findById(patrocinadorId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("La persona que refiere no existe."));
        String documentoNormalizado = requerido(documento, "El CI es obligatorio.");

        preinscripcionReferidoDao
                .findByDocumentoAndEstadoPreinscripcion(documentoNormalizado, PreinscripcionReferido.ESTADO_PREINSCRIPCION_PENDIENTE)
                .ifPresent(item -> {
                    throw new IllegalArgumentException("Ya existe una preinscripcion pendiente con ese CI.");
                });

        return preinscripcionReferidoDao.save(PreinscripcionReferido.builder()
                .patrocinador(patrocinador)
                .nombres(requerido(nombres, "Los nombres son obligatorios."))
                .apellidos(requerido(apellidos, "Los apellidos son obligatorios."))
                .documento(documentoNormalizado)
                .telefono(requerido(telefono, "El numero de celular es obligatorio."))
                .email(normalizar(email))
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

    private String normalizar(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
