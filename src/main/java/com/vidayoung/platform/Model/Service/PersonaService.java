package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Persona;
import java.util.List;
import java.util.Optional;

public interface PersonaService {

    List<Persona> listar();

    Optional<Persona> buscarPorId(Long id);

    Optional<Persona> buscarPorDocumento(String documento);

    Persona guardar(Persona persona);

    void eliminar(Long id);

    boolean existePorDocumento(String documento);
}
