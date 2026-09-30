package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.RangoDao;
import com.vidayoung.platform.Model.Dao.RangoNivelDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Rango;
import com.vidayoung.platform.Model.Entity.RangoNivel;
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

    private final RangoNivelDao rangoNivelDao;

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
        if (rango.getColor() == null || rango.getColor().trim().isEmpty()) {
            rango.setColor(colorPorDefecto(rango.getNombre()));
        }
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

        if (rango.getNivelesExtra() == null) {
            rango.setNivelesExtra(0);
        }

        if (rango.getNivelesExtra() < 0) {
            throw new IllegalArgumentException("Los niveles extra no pueden ser negativos.");
        }
    }

    private String colorPorDefecto(String nombre) {
        String base = nombre == null ? "" : nombre.trim().toUpperCase();
        if (base.contains("BRONCE")) return "#B0703A";
        if (base.contains("PLATA")) return "#9AA3B2";
        if (base.contains("ORO")) return "#C9A227";
        if (base.contains("DIAMANTE")) return "#38BDF8";
        if (base.contains("ESMERALDA")) return "#10B981";
        if (base.contains("RUB")) return "#E11D48";
        if (base.contains("ZAFIRO")) return "#2563EB";
        return "#F28705";
    }

    @Override
    public List<RangoNivel> listarNiveles(Long rangoId) {
        return rangoNivelDao.findByRangoId(rangoId).stream()
                .filter(nivel -> Auditoria.ESTADO_ACTIVO.equals(nivel.getEstado()))
                .sorted(Comparator.comparing(RangoNivel::getNumeroNivelExtra))
                .toList();
    }

    @Override
    public RangoNivel guardarNivel(Long rangoId, RangoNivel nivel) {
        Rango rango = buscarPorId(rangoId)
                .orElseThrow(() -> new IllegalArgumentException("El rango no existe."));

        if (nivel.getNumeroNivelExtra() == null || nivel.getNumeroNivelExtra() < 1) {
            throw new IllegalArgumentException("El numero de nivel extra debe ser mayor a cero.");
        }

        int extraMax = rango.getNivelesExtra() == null ? 0 : rango.getNivelesExtra();
        if (extraMax > 0 && nivel.getNumeroNivelExtra() > extraMax) {
            throw new IllegalArgumentException("El nivel extra supera los niveles extra del rango (" + extraMax + ").");
        }

        if (nivel.getMontoPorProducto() != null && nivel.getMontoPorProducto().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto por producto no puede ser negativo.");
        }

        RangoNivel persistente = rangoNivelDao
                .findByRangoIdAndNumeroNivelExtra(rangoId, nivel.getNumeroNivelExtra())
                .orElse(nivel);
        persistente.setRango(rango);
        persistente.setNumeroNivelExtra(nivel.getNumeroNivelExtra());
        persistente.setMontoPorProducto(nivel.getMontoPorProducto() == null ? BigDecimal.ZERO : nivel.getMontoPorProducto());
        persistente.setEstado(Auditoria.ESTADO_ACTIVO);

        return rangoNivelDao.save(persistente);
    }

    @Override
    public void eliminarNivel(Long nivelId) {
        rangoNivelDao.findById(nivelId).ifPresent(nivel -> {
            nivel.setEstado(Auditoria.ESTADO_ELIMINADO);
            rangoNivelDao.save(nivel);
        });
    }
}
