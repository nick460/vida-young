package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.PresenciaResponse;
import java.util.List;

public interface PresenciaService {

    void registrarLatido(String username, String ip, String userAgent, String rutaActual);

    void marcarSalida(String username);

    List<PresenciaResponse> listarEnLinea(int minutos);

    long contarEnLinea(int minutos);
}
