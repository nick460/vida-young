package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    List<Usuario> listar();

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorUsername(String username);

    Usuario guardar(Usuario usuario);

    Usuario actualizarFotoPerfil(Long id, String fotoPerfil);

    void eliminar(Long id);

    boolean existePorUsername(String username);
}
