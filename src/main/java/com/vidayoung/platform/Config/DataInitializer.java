package com.vidayoung.platform.Config;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.RolDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Entity.Usuario;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String DEFAULT_ROLE = "ADMIN";

    private final PersonaDao personaDao;
    private final UsuarioDao usuarioDao;
    private final RolDao rolDao;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDefaultUser() {
        return args -> {
            Persona persona = personaDao.findByDocumento("00000000")
                    .orElseGet(() -> Persona.builder()
                            .nombres("Administrador")
                            .apellidos("Vidayoung")
                            .documento("00000000")
                            .email("admin@vidayoung.com")
                            .telefono("00000000")
                            .build());

            Rol rol = rolDao.findByNombre(DEFAULT_ROLE)
                    .orElseGet(() -> Rol.builder()
                            .nombre(DEFAULT_ROLE)
                            .descripcion("Rol administrador por defecto")
                            .build());

            Usuario usuario = usuarioDao.findByUsername(DEFAULT_USERNAME)
                    .orElseGet(() -> Usuario.builder()
                            .username(DEFAULT_USERNAME)
                            .persona(persona)
                            .build());

            usuario.setActivo(true);
            usuario.setEstado(Auditoria.ESTADO_ACTIVO);
            usuario.setPersona(persona);

            if (!isBCryptHash(usuario.getPassword())) {
                usuario.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            }

            Set<Rol> roles = new HashSet<>(usuario.getRoles() == null ? Set.of() : usuario.getRoles());
            roles.add(rol);
            usuario.setRoles(roles);

            persona.setUsuario(usuario);
            persona.setEstado(Auditoria.ESTADO_ACTIVO);
            rol.setEstado(Auditoria.ESTADO_ACTIVO);

            usuarioDao.save(usuario);
        };
    }

    private boolean isBCryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
