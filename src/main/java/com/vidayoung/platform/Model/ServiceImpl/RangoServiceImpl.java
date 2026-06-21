package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.RangoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Service.RangoService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RangoServiceImpl implements RangoService {

    private final RangoDao rangoDao;

    @Override
    public List<Rango> listar() {
        return rangoDao.findAll().stream()
                .filter(rango -> Auditoria.ESTADO_ACTIVO.equals(rango.getEstado()))
                .sorted(Comparator.comparing(Rango::getQpMinimo))
                .toList();
    }

    @Override
    public Optional<Rango> buscarPorId(Long id) {
        return rangoDao.findById(id)
                .filter(rango -> Auditoria.ESTADO_ACTIVO.equals(rango.getEstado()));
    }

    @Override
    public Rango guardar(Rango rango) {
        validar(rango);
        rango.setNombre(rango.getNombre().trim());
        return rangoDao.save(rango);
    }

    @Override
    public void eliminar(Long id) {
        rangoDao.findById(id).ifPresent(rango -> {
            rango.setEstado(Auditoria.ESTADO_ELIMINADO);
            rangoDao.save(rango);
        });
    }

    private void validar(Rango rango) {
        if (rango.getNombre() == null || rango.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del rango es obligatorio.");
        }

        if (rango.getQpMinimo() == null) {
            rango.setQpMinimo(BigDecimal.ZERO);
        }

        if (rango.getQpMinimo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El QP minimo no puede ser negativo.");
        }
    }
}
