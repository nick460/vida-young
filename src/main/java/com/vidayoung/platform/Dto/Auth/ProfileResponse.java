package com.vidayoung.platform.Dto.Auth;

import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Entity.Usuario;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {

    private Long usuarioId;

    private String username;

    private Boolean activo;

    private String fotoPerfil;

    private PersonaData persona;

    private List<String> roles;

    public static ProfileResponse desdeUsuario(Usuario usuario) {
        Persona persona = usuario.getPersona();
        PersonaData personaData = persona == null ? null : new PersonaData(
                persona.getId(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getDocumento(),
                persona.getEmail(),
                persona.getTelefono()
        );

        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .toList();

        return new ProfileResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getActivo(),
                usuario.getFotoPerfil(),
                personaData,
                roles
        );
    }

    @Getter
    @AllArgsConstructor
    public static class PersonaData {

        private Long id;

        private String nombres;

        private String apellidos;

        private String documento;

        private String email;

        private String telefono;
    }
}
