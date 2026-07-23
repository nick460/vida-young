package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.Asistente.AsistenteChatRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteChatResponse;

public interface AsistenteService {

    AsistenteChatResponse enviarMensaje(AsistenteChatRequest request);
}
