package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Rol;
import java.util.List;
import java.util.Optional;

public interface RolService {

    List<Rol> listar();

    Optional<Rol> buscarPorId(Long id);

    Optional<Rol> buscarPorNombre(String nombre);

    Rol guardar(Rol rol);

    void eliminar(Long id);

    boolean existePorNombre(String nombre);
}
