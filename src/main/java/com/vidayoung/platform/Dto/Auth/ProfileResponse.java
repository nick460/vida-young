package com.vidayoung.platform.Dto.Auth;

import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Plan;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Entity.Usuario;
import java.time.LocalDateTime;
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

    private ReferidoData referido;

    private List<String> roles;

    public static ProfileResponse desdeUsuario(Usuario usuario) {
        return desdeUsuario(usuario, null, 0, 0);
    }

    public static ProfileResponse desdeUsuario(Usuario usuario, Referido referido, long referidosDirectos, long redTotal) {
        Persona persona = usuario.getPersona();
        PersonaData personaData = persona == null ? null : new PersonaData(
                persona.getId(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getDocumento(),
                persona.getEmail(),
                persona.getTelefono(),
                persona.getRangoActual() == null ? null : persona.getRangoActual().getNombre()
        );
        ReferidoData referidoData = referido == null ? null : toReferidoData(referido, referidosDirectos, redTotal);

        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .toList();

        return new ProfileResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getActivo(),
                usuario.getFotoPerfil(),
                personaData,
                referidoData,
                roles
        );
    }

    private static ReferidoData toReferidoData(Referido referido, long referidosDirectos, long redTotal) {
        Persona patrocinador = referido.getPatrocinador();
        Plan plan = referido.getPlan();

        return new ReferidoData(
                referido.getId(),
                referido.getFechaUnion(),
                referido.getFechaInicioMembresia(),
                referido.getFechaFinMembresia(),
                referido.getMembresiaActiva(),
                patrocinador == null ? null : new PersonaData(
                        patrocinador.getId(),
                        patrocinador.getNombres(),
                        patrocinador.getApellidos(),
                        patrocinador.getDocumento(),
                        patrocinador.getEmail(),
                        patrocinador.getTelefono(),
                        patrocinador.getRangoActual() == null ? null : patrocinador.getRangoActual().getNombre()
                ),
                plan == null ? null : new PlanData(plan.getId(), plan.getNombre()),
                referidosDirectos,
                redTotal
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

        private String rangoActual;
    }

    @Getter
    @AllArgsConstructor
    public static class ReferidoData {

        private Long id;

        private LocalDateTime fechaUnion;

        private LocalDateTime fechaInicioMembresia;

        private LocalDateTime fechaFinMembresia;

        private Boolean membresiaActiva;

        private PersonaData patrocinador;

        private PlanData plan;

        private long referidosDirectos;

        private long redTotal;
    }

    @Getter
    @AllArgsConstructor
    public static class PlanData {

        private Long id;

        private String nombre;
    }
}
