package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.UsuarioService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioServiceIplem implements UsuarioService {

    private final UsuarioDao usuarioDao;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Usuario> listar() {
        return usuarioDao.findAll().stream()
                .filter(usuario -> Auditoria.ESTADO_ACTIVO.equals(usuario.getEstado()))
                .toList();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioDao.findById(id)
                .filter(usuario -> Auditoria.ESTADO_ACTIVO.equals(usuario.getEstado()));
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioDao.findByUsername(username)
                .filter(usuario -> Auditoria.ESTADO_ACTIVO.equals(usuario.getEstado()));
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioDao.save(usuario);
    }

    @Override
    public Usuario actualizarFotoPerfil(Long id, String fotoPerfil) {
        Usuario usuario = usuarioDao.findById(id)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setFotoPerfil(fotoPerfil);
        return usuarioDao.save(usuario);
    }

    @Override
    public void eliminar(Long id) {
        usuarioDao.findById(id).ifPresent(usuario -> {
            usuario.setEstado(Auditoria.ESTADO_ELIMINADO);
            usuarioDao.save(usuario);
        });
    }

    @Override
    public boolean existePorUsername(String username) {
        return usuarioDao.existsByUsername(username);
    }
}
