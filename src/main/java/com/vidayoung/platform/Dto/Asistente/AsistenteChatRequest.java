package com.vidayoung.platform.Dto.Asistente;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsistenteChatRequest {

    private String message;

    private List<AsistenteMessage> history = new ArrayList<>();
}
