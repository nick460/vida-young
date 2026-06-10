package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.PlanDao;
import com.vidayoung.platform.Model.Dao.PlanNivelDao;
import com.vidayoung.platform.Model.Dao.PlanProductoDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanNivel;
import com.vidayoung.platform.Model.Entity.PlanProducto;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Service.PlanService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanServiceIplem implements PlanService {

    private final PlanDao planDao;
    private final PlanNivelDao planNivelDao;
    private final PlanProductoDao planProductoDao;
    private final ProductoDao productoDao;

    @Override
    public List<Plan> listar() {
        return planDao.findAll().stream()
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()))
                .toList();
    }

    @Override
    public Optional<Plan> buscarPorId(Long id) {
        return planDao.findById(id)
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()));
    }

    @Override
    public Optional<Plan> buscarPorNombre(String nombre) {
        return planDao.findByNombre(nombre)
                .filter(plan -> Auditoria.ESTADO_ACTIVO.equals(plan.getEstado()));
    }

    @Override
    public Plan guardar(Plan plan) {
        validarPlan(plan);
        return planDao.save(plan);
    }

    @Override
    @Transactional
    public PlanNivel guardarNivel(Long planId, PlanNivel nivel) {
        Plan plan = buscarPorId(planId)
                .orElseThrow(() -> new IllegalArgumentException("El plan no existe o no esta activo"));

        validarNivel(plan, nivel);

        PlanNivel nivelPersistente = planNivelDao.findByPlanIdAndNumeroNivel(planId, nivel.getNumeroNivel())
                .orElse(nivel);
        nivelPersistente.setPlan(plan);
        nivelPersistente.setNumeroNivel(nivel.getNumeroNivel());
        nivelPersistente.setPorcentajeComision(nivel.getPorcentajeComision());
        nivelPersistente.setValorProductosBeneficio(nivel.getValorProductosBeneficio() == null
                ? BigDecimal.ZERO
                : nivel.getValorProductosBeneficio());
        nivelPersistente.setEstado(Auditoria.ESTADO_ACTIVO);

        return planNivelDao.save(nivelPersistente);
    }

    @Override
    @Transactional
    public PlanProducto guardarProducto(Long planId, Long productoId, Integer cantidad) {
        Plan plan = buscarPorId(planId)
                .orElseThrow(() -> new IllegalArgumentException("El plan no existe o no esta activo"));
        Producto producto = productoDao.findById(productoId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe o no esta activo"));

        if (cantidad == null || cantidad < 1) {
            throw new IllegalArgumentException("La cantidad del producto debe ser mayor a cero");
        }

        PlanProducto planProducto = planProductoDao.findByPlanIdAndProductoId(planId, productoId)
                .orElse(PlanProducto.builder().build());
        planProducto.setPlan(plan);
        planProducto.setProducto(producto);
        planProducto.setCantidad(cantidad);
        planProducto.setEstado(Auditoria.ESTADO_ACTIVO);

        return planProductoDao.save(planProducto);
    }

    @Override
    public void eliminarNivel(Long nivelId) {
        planNivelDao.findById(nivelId).ifPresent(nivel -> {
            nivel.setEstado(Auditoria.ESTADO_ELIMINADO);
            planNivelDao.save(nivel);
        });
    }

    @Override
    public void eliminarProducto(Long planProductoId) {
        planProductoDao.findById(planProductoId).ifPresent(planProducto -> {
            planProducto.setEstado(Auditoria.ESTADO_ELIMINADO);
            planProductoDao.save(planProducto);
        });
    }

    @Override
    public void eliminar(Long id) {
        planDao.findById(id).ifPresent(plan -> {
            plan.setEstado(Auditoria.ESTADO_ELIMINADO);
            planDao.save(plan);
        });
    }

    private void validarPlan(Plan plan) {
        if (plan.getPrecio() == null || plan.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del plan no puede ser negativo");
        }

        if (plan.getBonificacionDirecta() == null) {
            plan.setBonificacionDirecta(BigDecimal.ZERO);
        }

        if (plan.getQp() == null) {
            plan.setQp(BigDecimal.ZERO);
        }

        if (plan.getValorProductosBeneficio() == null) {
            plan.setValorProductosBeneficio(BigDecimal.ZERO);
        }

        if (plan.getQp().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El QP del plan no puede ser negativo");
        }

        if (plan.getBonificacionDirecta().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La bonificacion directa no puede ser negativa");
        }

        if (plan.getValorProductosBeneficio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El valor de productos de beneficio no puede ser negativo");
        }

        if (plan.getNivelesAlcance() == null || plan.getNivelesAlcance() < 1) {
            throw new IllegalArgumentException("El plan debe alcanzar al menos un nivel");
        }
    }

    private void validarNivel(Plan plan, PlanNivel nivel) {
        if (nivel.getNumeroNivel() == null || nivel.getNumeroNivel() < 1) {
            throw new IllegalArgumentException("El numero de nivel debe ser mayor a cero");
        }

        if (nivel.getNumeroNivel() > plan.getNivelesAlcance()) {
            throw new IllegalArgumentException("El nivel supera el alcance configurado para el plan");
        }

        if (nivel.getPorcentajeComision() == null
                || nivel.getPorcentajeComision().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto de recompensa debe ser mayor o igual a cero");
        }

        if (nivel.getValorProductosBeneficio() != null
                && nivel.getValorProductosBeneficio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El valor en productos debe ser mayor o igual a cero");
        }
    }
}
