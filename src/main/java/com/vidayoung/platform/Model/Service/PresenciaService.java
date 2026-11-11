package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.HistorialAccesoResponse;
import com.vidayoung.platform.Dto.PresenciaResponse;
import java.util.List;

public interface PresenciaService {

    void registrarLatido(String username, String ip, String userAgent, String rutaActual);

    void marcarSalida(String username);

    void registrarIngreso(String username, String ip, String userAgent, String rutaInicial);

    List<PresenciaResponse> listarEnLinea(int minutos);

    long contarEnLinea(int minutos);

    List<HistorialAccesoResponse> listarHistorial(int limite);
}
