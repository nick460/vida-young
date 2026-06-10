package com.vidayoung.platform.Config;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.RolDao;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Entity.Usuario;
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
            boolean hasData = usuarioDao.count() > 0
                    || personaDao.count() > 0
                    || rolDao.count() > 0;

            if (hasData) {
                return;
            }

            Persona persona = Persona.builder()
                    .nombres("Administrador")
                    .apellidos("Vidayoung")
                    .documento("00000000")
                    .email("admin@vidayoung.com")
                    .telefono("00000000")
                    .build();

            Rol rol = Rol.builder()
                    .nombre(DEFAULT_ROLE)
                    .descripcion("Rol administrador por defecto")
                    .build();

            Usuario usuario = Usuario.builder()
                    .username(DEFAULT_USERNAME)
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .activo(true)
                    .persona(persona)
                    .roles(Set.of(rol))
                    .build();

            persona.setUsuario(usuario);
            rol.setUsuarios(Set.of(usuario));

            usuarioDao.save(usuario);
        };
    }
}