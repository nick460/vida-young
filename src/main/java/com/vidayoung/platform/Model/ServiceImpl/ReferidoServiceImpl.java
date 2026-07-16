package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.PlanNivelDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanNivel;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.BilleteraService;
import com.vidayoung.platform.Model.Service.ReferidoService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferidoServiceImpl implements ReferidoService {

    private final ReferidoDao referidoDao;
    private final PersonaDao personaDao;
    private final PlanDao planDao;
    private final PlanNivelDao planNivelDao;
    private final RecompensaDao recompensaDao;
    private final BilleteraService billeteraService;
    private final UsuarioDao usuarioDao;

    @Override
    public List<Referido> listar() {
        return referidoDao.findAll().stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .peek(this::hidratarFotos)
                .toList();
    }

    @Override
    public Optional<Referido> buscarPorId(Long id) {
        return referidoDao.findById(id)
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .map(this::hidratarFotos);
    }

    @Override
    public Optional<Referido> buscarPorPersonaId(Long personaId) {
        return referidoDao.findByPersonaId(personaId)
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .map(this::hidratarFotos);
    }

    @Override
    public List<Referido> listarPorPatrocinador(Long patrocinadorId) {
        return referidoDao.findByPatrocinadorId(patrocinadorId).stream()
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .peek(this::hidratarFotos)
                .toList();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Referido guardar(Long personaId, Long patrocinadorId, Long planId) {
        Optional<Referido> existente = referidoDao.findByPersonaId(personaId);

        if (existente
                .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                .isPresent()) {
            throw new IllegalArgumentException("La persona ya esta registrada en la red.");
        }

        Referido referido = referidoDao.save(buildReferido(existente.orElseGet(Referido::new), personaId, patrocinadorId, planId));
        billeteraService.registrarAfiliacionInicial(referido);
        regenerarRecompensas(referido);
        return referido;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Referido actualizar(Long id, Long personaId, Long patrocinadorId, Long planId) {
        Referido referido = buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Referido no encontrado."));

        buscarPorPersonaId(personaId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("La persona ya esta registrada en la red.");
                });

        Referido actualizado = referidoDao.save(buildReferido(referido, personaId, patrocinadorId, planId));
        billeteraService.registrarAfiliacionInicial(actualizado);
        regenerarRecompensas(actualizado);
        return actualizado;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void eliminar(Long id) {
        referidoDao.findById(id).ifPresent(referido -> {
            Persona patrocinadorInmediato = referido.getPatrocinador();

            referidoDao.findByPatrocinadorId(referido.getPersona().getId()).stream()
                    .filter(hijo -> Auditoria.ESTADO_ACTIVO.equals(hijo.getEstado()))
                    .forEach(hijo -> {
                        hijo.setPatrocinador(patrocinadorInmediato);
                        Referido hijoActualizado = referidoDao.save(hijo);
                        regenerarRecompensas(hijoActualizado);
                    });

            referido.setEstado(Auditoria.ESTADO_ELIMINADO);
            referidoDao.save(referido);
            desactivarRecompensas(referido);
        });
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public int vencerMembresiasExpiradas() {
        List<Referido> vencidos = referidoDao.findByMembresiaActivaTrueAndFechaFinMembresiaLessThanEqual(LocalDateTime.now());

        vencidos.forEach(referido -> {
            referido.setMembresiaActiva(false);
            referidoDao.save(referido);
        });

        billeteraService.vencerHistorialMembresiasExpiradas();

        return vencidos.size();
    }

    private Referido buildReferido(Referido referido, Long personaId, Long patrocinadorId, Long planId) {
        if (personaId == null || planId == null) {
            throw new IllegalArgumentException("Persona y plan son obligatorios.");
        }

        if (personaId.equals(patrocinadorId)) {
            throw new IllegalArgumentException("Una persona no puede ser su propio patrocinador.");
        }

        Persona persona = personaDao.findById(personaId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));
        Persona patrocinador = null;

        if (patrocinadorId != null) {
            patrocinador = personaDao.findById(patrocinadorId)
                    .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                    .orElseThrow(() -> new IllegalArgumentException("Patrocinador no encontrado."));
        }

        Plan plan = planDao.findById(planId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado."));

        referido.setPersona(persona);
        referido.setPatrocinador(patrocinador);
        referido.setPlan(plan);
        referido.setEstado(Auditoria.ESTADO_ACTIVO);

        if (referido.getFechaUnion() == null) {
            LocalDateTime fechaUnion = LocalDateTime.now();
            referido.setFechaUnion(fechaUnion);
            referido.setFechaInicioMembresia(fechaUnion);
            referido.setFechaFinMembresia(calcularFechaFinMembresia(fechaUnion));
            referido.setMembresiaActiva(true);
        } else {
            if (referido.getFechaInicioMembresia() == null) {
                referido.setFechaInicioMembresia(referido.getFechaUnion());
            }

            if (referido.getFechaFinMembresia() == null) {
                referido.setFechaFinMembresia(calcularFechaFinMembresia(referido.getFechaInicioMembresia()));
            }

            if (referido.getMembresiaActiva() == null) {
                referido.setMembresiaActiva(!referido.getFechaFinMembresia().isBefore(LocalDateTime.now()));
            }
        }

        return referido;
    }

    private LocalDateTime calcularFechaFinMembresia(LocalDateTime fechaInicio) {
        LocalDate fechaFin = fechaInicio.toLocalDate().plusMonths(1);
        return LocalDateTime.of(fechaFin, LocalTime.of(23, 59, 59));
    }

    private void regenerarRecompensas(Referido referido) {
        desactivarRecompensas(referido);

        if (referido.getPatrocinador() == null || referido.getPlan() == null) {
            return;
        }

        int alcance = Optional.ofNullable(referido.getPlan().getNivelesAlcance()).orElse(0);

        if (alcance < 1) {
            return;
        }

        Map<Integer, PlanNivel> nivelesPorNumero = planNivelDao.findByPlanId(referido.getPlan().getId()).stream()
                .filter(nivel -> Auditoria.ESTADO_ACTIVO.equals(nivel.getEstado()))
                .collect(Collectors.toMap(PlanNivel::getNumeroNivel, Function.identity(), (left, right) -> left));

        Persona beneficiario = referido.getPatrocinador();
        int nivelActual = 1;

        while (beneficiario != null && nivelActual <= alcance) {
            PlanNivel nivel = nivelesPorNumero.get(nivelActual);
            BigDecimal efectivo = nivel == null ? BigDecimal.ZERO : zeroIfNull(nivel.getPorcentajeComision());
            BigDecimal productos = nivel == null ? BigDecimal.ZERO : zeroIfNull(nivel.getValorProductosBeneficio());
            Optional<Referido> beneficiarioReferido = referidoDao.findByPersonaId(beneficiario.getId())
                    .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()));
            int alcanceBeneficiario = beneficiarioReferido
                    .map(Referido::getPlan)
                    .map(Plan::getNivelesAlcance)
                    .orElse(0);
            boolean nivelAlcanza = alcanceBeneficiario >= nivelActual;
            boolean beneficiarioActivo = beneficiarioReferido
                    .map(this::membresiaActiva)
                    .orElse(false);

            if (nivelAlcanza && (efectivo.compareTo(BigDecimal.ZERO) > 0 || productos.compareTo(BigDecimal.ZERO) > 0)) {
                Recompensa recompensa = recompensaDao.save(Recompensa.builder()
                        .referido(referido)
                        .beneficiario(beneficiario)
                        .planIngreso(referido.getPlan())
                        .nivelGenerado(nivelActual)
                        .montoEfectivo(efectivo)
                        .valorProductos(productos)
                        .cobrable(beneficiarioActivo)
                        .motivoNoCobrable(beneficiarioActivo ? null : "No cobrable porque la membresia no esta activa.")
                        .build());
                billeteraService.sincronizarSaldoProductosRecompensa(recompensa.getId());
            }

            beneficiario = beneficiarioReferido
                    .map(Referido::getPatrocinador)
                    .orElse(null);
            nivelActual++;
        }
    }

    private boolean membresiaActiva(Referido referido) {
        return Boolean.TRUE.equals(referido.getMembresiaActiva())
                && referido.getFechaFinMembresia() != null
                && !referido.getFechaFinMembresia().isBefore(LocalDateTime.now());
    }

    private void desactivarRecompensas(Referido referido) {
        if (referido.getId() == null) {
            return;
        }

        recompensaDao.findByReferidoId(referido.getId()).forEach(recompensa -> {
            recompensa.setEstado(Auditoria.ESTADO_ELIMINADO);
            recompensa = recompensaDao.save(recompensa);
            billeteraService.sincronizarSaldoProductosRecompensa(recompensa.getId());
        });
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Referido hidratarFotos(Referido referido) {
        hidratarFoto(referido.getPersona());
        hidratarFoto(referido.getPatrocinador());
        return referido;
    }

    private void hidratarFoto(Persona persona) {
        if (persona == null || persona.getId() == null) {
            return;
        }

        usuarioDao.findByPersonaId(persona.getId())
                .map(usuario -> usuario.getFotoPerfil())
                .ifPresent(persona::setFotoPerfil);
    }
}
