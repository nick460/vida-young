package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Referido;
import java.util.List;
import java.util.Optional;

public interface ReferidoService {

    List<Referido> listar();

    Optional<Referido> buscarPorId(Long id);

    Optional<Referido> buscarPorPersonaId(Long personaId);

    List<Referido> listarPorPatrocinador(Long patrocinadorId);

    Referido guardar(Long personaId, Long patrocinadorId, Long planId);

    Referido actualizar(Long id, Long personaId, Long patrocinadorId, Long planId);

    void eliminar(Long id);

    int vencerMembresiasExpiradas();

    int desactivarMembresiasActivas();
}
