package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Dto.PlanActivacionProyeccionResponse;
import com.vidayoung.platform.Model.Dao.BilleteraDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionDao;
import com.vidayoung.platform.Model.Dao.PlanActivacionNivelDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.PlanActivacionService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanActivacionServiceImpl implements PlanActivacionService {

    private final PlanActivacionDao planActivacionDao;
    private final PlanActivacionNivelDao planActivacionNivelDao;
    private final BilleteraDao billeteraDao;
    private final ReferidoDao referidoDao;

    @Override
    public List<PlanActivacion> listar() {
        return planActivacionDao.findAll().stream()
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .sorted(Comparator.comparing(PlanActivacion::getPvMinimoMensual))
                .toList();
    }

    @Override
    public Optional<PlanActivacion> buscarPorId(Long id) {
        return planActivacionDao.findById(id)
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()));
    }

    @Override
    public PlanActivacion guardar(PlanActivacion planActivacion) {
        validarPlan(planActivacion);
        return planActivacionDao.save(planActivacion);
    }

    @Override
    @Transactional
    public PlanActivacionNivel guardarNivel(Long planActivacionId, PlanActivacionNivel nivel) {
        PlanActivacion plan = buscarPorId(planActivacionId)
                .orElseThrow(() -> new IllegalArgumentException("El plan de activacion no existe."));

        validarNivel(plan, nivel);

        PlanActivacionNivel persistente = planActivacionNivelDao
                .findByPlanActivacionIdAndNumeroNivel(planActivacionId, nivel.getNumeroNivel())
                .orElse(nivel);
        persistente.setPlanActivacion(plan);
        persistente.setNumeroNivel(nivel.getNumeroNivel());
        persistente.setMontoPorProducto(zeroIfNull(nivel.getMontoPorProducto()));
        persistente.setEstado(Auditoria.ESTADO_ACTIVO);

        return planActivacionNivelDao.save(persistente);
    }

    @Override
    public void eliminarNivel(Long nivelId) {
        planActivacionNivelDao.findById(nivelId).ifPresent(nivel -> {
            nivel.setEstado(Auditoria.ESTADO_ELIMINADO);
            planActivacionNivelDao.save(nivel);
        });
    }

    @Override
    public void eliminar(Long id) {
        planActivacionDao.findById(id).ifPresent(plan -> {
            plan.setEstado(Auditoria.ESTADO_ELIMINADO);
            planActivacionDao.save(plan);
        });
    }

    @Override
    public PlanActivacionProyeccionResponse proyectarPorPersona(Long personaId) {
        BigDecimal pvActual = billeteraDao.findByPersonaId(personaId)
                .map(Billetera::getSaldoPv)
                .map(this::zeroIfNull)
                .orElse(BigDecimal.ZERO);
        Map<Integer, Integer> personasPorNivel = contarDescendientesPorNivel(personaId);

        List<PlanActivacionProyeccionResponse.PlanProyectado> planes = listar().stream()
                .map(plan -> proyectarPlan(plan, pvActual, personasPorNivel))
                .toList();

        return new PlanActivacionProyeccionResponse(personaId, pvActual, planes);
    }

    private PlanActivacionProyeccionResponse.PlanProyectado proyectarPlan(
            PlanActivacion plan,
            BigDecimal pvActual,
            Map<Integer, Integer> personasPorNivel
    ) {
        Map<Integer, PlanActivacionNivel> niveles = planActivacionNivelDao.findByPlanActivacionId(plan.getId()).stream()
                .filter(nivel -> Auditoria.ESTADO_ACTIVO.equals(nivel.getEstado()))
                .collect(Collectors.toMap(PlanActivacionNivel::getNumeroNivel, Function.identity(), (left, right) -> left));

        List<PlanActivacionProyeccionResponse.NivelProyectado> nivelesProyectados = java.util.stream.IntStream
                .rangeClosed(1, Math.max(1, plan.getNivelesAlcance()))
                .mapToObj(numeroNivel -> {
                    int personas = personasPorNivel.getOrDefault(numeroNivel, 0);
                    BigDecimal monto = zeroIfNull(niveles.get(numeroNivel) == null ? BigDecimal.ZERO : niveles.get(numeroNivel).getMontoPorProducto());
                    return new PlanActivacionProyeccionResponse.NivelProyectado(
                            numeroNivel,
                            personas,
                            monto,
                            monto.multiply(BigDecimal.valueOf(personas))
                    );
                })
                .toList();

        BigDecimal total = nivelesProyectados.stream()
                .map(PlanActivacionProyeccionResponse.NivelProyectado::getPotencialPorProducto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pvMinimo = zeroIfNull(plan.getPvMinimoMensual());
        boolean activable = pvActual.compareTo(pvMinimo) >= 0;

        return new PlanActivacionProyeccionResponse.PlanProyectado(
                plan.getId(),
                plan.getNombre(),
                pvMinimo,
                plan.getNivelesAlcance(),
                activable,
                activable ? BigDecimal.ZERO : pvMinimo.subtract(pvActual),
                total,
                nivelesProyectados
        );
    }

    private Map<Integer, Integer> contarDescendientesPorNivel(Long personaId) {
        Map<Integer, Integer> conteo = new HashMap<>();
        Queue<NodoRed> cola = new ArrayDeque<>();
        cola.add(new NodoRed(personaId, 0));

        while (!cola.isEmpty()) {
            NodoRed actual = cola.poll();
            List<Referido> directos = referidoDao.findByPatrocinadorId(actual.personaId()).stream()
                    .filter(referido -> Auditoria.ESTADO_ACTIVO.equals(referido.getEstado()))
                    .toList();

            int siguienteNivel = actual.nivel() + 1;
            if (!directos.isEmpty()) {
                conteo.merge(siguienteNivel, directos.size(), Integer::sum);
            }

            directos.forEach(referido -> cola.add(new NodoRed(referido.getPersona().getId(), siguienteNivel)));
        }

        return conteo;
    }

    private void validarPlan(PlanActivacion planActivacion) {
        if (planActivacion.getPvMinimoMensual() == null) {
            planActivacion.setPvMinimoMensual(BigDecimal.ZERO);
        }

        if (planActivacion.getPvMinimoMensual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El PV minimo mensual no puede ser negativo.");
        }

        if (planActivacion.getNivelesAlcance() == null || planActivacion.getNivelesAlcance() < 1) {
            throw new IllegalArgumentException("El plan de activacion debe alcanzar al menos un nivel.");
        }
    }

    private void validarNivel(PlanActivacion plan, PlanActivacionNivel nivel) {
        if (nivel.getNumeroNivel() == null || nivel.getNumeroNivel() < 1) {
            throw new IllegalArgumentException("El numero de nivel debe ser mayor a cero.");
        }

        if (nivel.getNumeroNivel() > plan.getNivelesAlcance()) {
            throw new IllegalArgumentException("El nivel supera el alcance del plan de activacion.");
        }

        if (nivel.getMontoPorProducto() != null && nivel.getMontoPorProducto().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto por producto no puede ser negativo.");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record NodoRed(Long personaId, int nivel) {
    }
}
