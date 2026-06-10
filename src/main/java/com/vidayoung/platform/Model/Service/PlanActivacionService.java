package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.PlanActivacionProyeccionResponse;
import com.vidayoung.platform.Model.Entity.PlanActivacion;
import com.vidayoung.platform.Model.Entity.PlanActivacionNivel;
import java.util.List;
import java.util.Optional;

public interface PlanActivacionService {

    List<PlanActivacion> listar();

    Optional<PlanActivacion> buscarPorId(Long id);

    PlanActivacion guardar(PlanActivacion planActivacion);

    PlanActivacionNivel guardarNivel(Long planActivacionId, PlanActivacionNivel nivel);

    void eliminarNivel(Long nivelId);

    void eliminar(Long id);

    PlanActivacionProyeccionResponse proyectarPorPersona(Long personaId);
}
