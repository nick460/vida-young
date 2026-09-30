package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.EvolutionApiConfig;

public interface EvolutionApiService {

    EvolutionApiConfig obtenerConfig();

    EvolutionApiConfig guardarConfig(EvolutionApiConfig config);

    boolean enviarBienvenida(String nombres, String telefono, String username, String passwordPlano);

}
