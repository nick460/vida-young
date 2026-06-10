package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Service.PersonaService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonaServiceIplem implements PersonaService {

    private final PersonaDao personaDao;

    @Override
    public List<Persona> listar() {
        return personaDao.findAll().stream()
                .filter(persona -> Auditoria.ESTADO_ACTIVO.equals(persona.getEstado()))
                .toList();
    }

    @Override
    public Optional<Persona> buscarPorId(Long id) {
        return personaDao.findById(id)
                .filter(persona -> Auditoria.ESTADO_ACTIVO.equals(persona.getEstado()));
    }

    @Override
    public Optional<Persona> buscarPorDocumento(String documento) {
        return personaDao.findByDocumento(documento);
    }

    @Override
    public Persona guardar(Persona persona) {
        return personaDao.save(persona);
    }

    @Override
    public void eliminar(Long id) {
        personaDao.findById(id).ifPresent(persona -> {
            persona.setEstado(Auditoria.ESTADO_ELIMINADO);
            personaDao.save(persona);
        });
    }

    @Override
    public boolean existePorDocumento(String documento) {
        return personaDao.existsByDocumento(documento);
    }
}
