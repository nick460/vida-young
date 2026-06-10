package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.PlanNivel;
import com.vidayoung.platform.Model.Entity.PlanProducto;
import java.util.List;
import java.util.Optional;

public interface PlanService {

    List<Plan> listar();

    Optional<Plan> buscarPorId(Long id);

    Optional<Plan> buscarPorNombre(String nombre);

    Plan guardar(Plan plan);

    PlanNivel guardarNivel(Long planId, PlanNivel nivel);

    PlanProducto guardarProducto(Long planId, Long productoId, Integer cantidad);

    void eliminarNivel(Long nivelId);

    void eliminarProducto(Long planProductoId);

    void eliminar(Long id);
}
