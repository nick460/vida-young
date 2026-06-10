package com.vidayoung.platform.Dto.Auth;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String tipo;

    private Long usuarioId;

    private String username;

    private List<String> roles;
}
