package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Gestion;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import java.util.List;
import java.util.Optional;

public interface GestionPeriodoService {

    List<Gestion> listarGestiones();

    List<PeriodoGestion> listarPeriodos(Long gestionId);

    Gestion crearGestion(Integer anio, String nombre);

    Gestion actualizarGestion(Long gestionId, Integer anio, String nombre);

    PeriodoGestion crearPeriodo(Long gestionId, Integer mes, String nombre);

    Optional<PeriodoGestion> buscarPeriodoActivo();

    PeriodoGestion obtenerPeriodoActivo();

    PeriodoGestion activarPeriodo(Long periodoId);

    PeriodoGestion cerrarPeriodoActivo();
}
