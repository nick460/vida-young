package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.RolDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Service.RolService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RolServiceIplem implements RolService {

    private final RolDao rolDao;

    @Override
    public List<Rol> listar() {
        return rolDao.findAll().stream()
                .filter(rol -> Auditoria.ESTADO_ACTIVO.equals(rol.getEstado()))
                .toList();
    }

    @Override
    public Optional<Rol> buscarPorId(Long id) {
        return rolDao.findById(id)
                .filter(rol -> Auditoria.ESTADO_ACTIVO.equals(rol.getEstado()));
    }

    @Override
    public Optional<Rol> buscarPorNombre(String nombre) {
        return rolDao.findByNombre(nombre)
                .filter(rol -> Auditoria.ESTADO_ACTIVO.equals(rol.getEstado()));
    }

    @Override
    public Rol guardar(Rol rol) {
        return rolDao.save(rol);
    }

    @Override
    public void eliminar(Long id) {
        rolDao.findById(id).ifPresent(rol -> {
            rol.setEstado(Auditoria.ESTADO_ELIMINADO);
            rolDao.save(rol);
        });
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return rolDao.existsByNombre(nombre);
    }
}
