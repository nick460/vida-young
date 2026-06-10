package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.LoteProductoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.LoteProducto;
import com.vidayoung.platform.Model.Service.LoteProductoService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoteProductoServiceIplem implements LoteProductoService {

    private final LoteProductoDao loteProductoDao;

    @Override
    public List<LoteProducto> listar() {
        return loteProductoDao.findAll().stream()
                .filter(lote -> Auditoria.ESTADO_ACTIVO.equals(lote.getEstado()))
                .toList();
    }

    @Override
    public List<LoteProducto> listarPorProducto(Long productoId) {
        return loteProductoDao.findByProductoId(productoId).stream()
                .filter(lote -> Auditoria.ESTADO_ACTIVO.equals(lote.getEstado()))
                .toList();
    }

    @Override
    public Optional<LoteProducto> buscarPorId(Long id) {
        return loteProductoDao.findById(id)
                .filter(lote -> Auditoria.ESTADO_ACTIVO.equals(lote.getEstado()));
    }

    @Override
    public LoteProducto guardar(LoteProducto loteProducto) {
        if (loteProducto.getCantidadDisponible() == null) {
            loteProducto.setCantidadDisponible(loteProducto.getCantidadInicial());
        }

        return loteProductoDao.save(loteProducto);
    }

    @Override
    public void eliminar(Long id) {
        loteProductoDao.findById(id).ifPresent(lote -> {
            lote.setEstado(Auditoria.ESTADO_ELIMINADO);
            loteProductoDao.save(lote);
        });
    }
}
