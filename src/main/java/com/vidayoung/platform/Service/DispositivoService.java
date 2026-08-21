package com.vidayoung.platform.Service;

import com.vidayoung.platform.Model.Entity.Dispositivo;
import com.vidayoung.platform.Model.Dao.DispositivoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DispositivoService {

    @Autowired
    private DispositivoDao dispositivoDao;

    public List<Dispositivo> findAll() {
        return dispositivoDao.findAll();
    }

    public Optional<Dispositivo> findByToken(String token) {
        return dispositivoDao.findByToken(token);
    }

    public List<Dispositivo> findByPersonaId(Long personaId) {
        return dispositivoDao.findByPersonaId(personaId);
    }

    @Transactional
    public Dispositivo save(Dispositivo dispositivo) {
        return dispositivoDao.save(dispositivo);
    }

    @Transactional
    public void deleteByToken(String token) {
        dispositivoDao.deleteByToken(token);
    }

    public boolean existsByToken(String token) {
        return dispositivoDao.existsByToken(token);
    }

    public long countByPersonaIdAndActivoTrue(Long personaId) {
        return dispositivoDao.countByPersonaIdAndActivoTrue(personaId);
    }
}