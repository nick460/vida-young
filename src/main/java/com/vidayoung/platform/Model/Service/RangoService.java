package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Entity.RangoNivel;
import java.util.List;
import java.util.Optional;

public interface RangoService {

    List<Rango> listar();

    Optional<Rango> buscarPorId(Long id);

    Rango guardar(Rango rango);

    void eliminar(Long id);

    List<RangoNivel> listarNiveles(Long rangoId);

    RangoNivel guardarNivel(Long rangoId, RangoNivel nivel);

    void eliminarNivel(Long nivelId);
}
