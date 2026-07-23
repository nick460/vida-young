package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.Asistente.AsistenteChatRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteChatResponse;
import com.vidayoung.platform.Dto.Asistente.AsistenteConfigRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteConfigResponse;

public interface AsistenteService {

    AsistenteChatResponse enviarMensaje(AsistenteChatRequest request);

    AsistenteConfigResponse obtenerConfiguracion();

    AsistenteConfigResponse guardarConfiguracion(AsistenteConfigRequest request);
}
