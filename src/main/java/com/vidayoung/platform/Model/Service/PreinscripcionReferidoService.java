package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.PreinscripcionReferido;
import java.util.List;

public interface PreinscripcionReferidoService {

    List<PreinscripcionReferido> listar(String estadoPreinscripcion);

    PreinscripcionReferido crear(Long patrocinadorId, String nombres, String apellidos, String documento, String telefono, String email);

    PreinscripcionReferido validar(Long id, Long patrocinadorId, Long planId, String nombres, String apellidos, String documento, String telefono, String email, String usuarioValidacion);

    PreinscripcionReferido rechazar(Long id, String observacion, String usuarioValidacion);
}
