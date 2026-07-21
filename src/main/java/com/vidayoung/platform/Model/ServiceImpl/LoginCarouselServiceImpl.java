package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.LoginCarouselItemDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.LoginCarouselItem;
import com.vidayoung.platform.Model.Service.LoginCarouselService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginCarouselServiceImpl implements LoginCarouselService {

    private final LoginCarouselItemDao loginCarouselItemDao;

    @Override
    public List<LoginCarouselItem> listar() {
        return loginCarouselItemDao.findAll().stream()
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .sorted(Comparator.comparing(LoginCarouselItem::getOrden, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LoginCarouselItem::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public List<LoginCarouselItem> listarActivos() {
        return loginCarouselItemDao.findByActivoTrueOrderByOrdenAscIdAsc().stream()
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .toList();
    }

    @Override
    public Optional<LoginCarouselItem> buscarPorId(Long id) {
        return loginCarouselItemDao.findById(id)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()));
    }

    @Override
    public LoginCarouselItem guardar(LoginCarouselItem item) {
        item.setTitulo(requerido(item.getTitulo(), "El titulo es obligatorio."));
        item.setDescripcion(requerido(item.getDescripcion(), "La descripcion es obligatoria."));
        item.setImagenUrl(requerido(item.getImagenUrl(), "La imagen es obligatoria."));
        item.setImagenMobileUrl(normalizar(item.getImagenMobileUrl()));
        item.setOrden(item.getOrden() == null ? 0 : item.getOrden());
        item.setActivo(item.getActivo() == null || item.getActivo());
        item.setEstado(Auditoria.ESTADO_ACTIVO);
        return loginCarouselItemDao.save(item);
    }

    @Override
    public void eliminar(Long id) {
        loginCarouselItemDao.findById(id).ifPresent(item -> {
            item.setEstado(Auditoria.ESTADO_ELIMINADO);
            loginCarouselItemDao.save(item);
        });
    }

    private String requerido(String value, String message) {
        String normalized = normalizar(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizar(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
