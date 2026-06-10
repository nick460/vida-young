package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Service.ProductoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductoServiceIplem implements ProductoService {

    private final ProductoDao productoDao;

    @Override
    public List<Producto> listar() {
        return productoDao.findAll().stream()
                .filter(producto -> Auditoria.ESTADO_ACTIVO.equals(producto.getEstado()))
                .toList();
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return productoDao.findById(id)
                .filter(producto -> Auditoria.ESTADO_ACTIVO.equals(producto.getEstado()));
    }

    @Override
    public Optional<Producto> buscarPorSku(String sku) {
        return productoDao.findBySku(sku)
                .filter(producto -> Auditoria.ESTADO_ACTIVO.equals(producto.getEstado()));
    }

    @Override
    public Producto guardar(Producto producto) {
        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(0);
        }

        if (producto.getPv() == null) {
            producto.setPv(BigDecimal.ZERO);
        }

        if (producto.getQp() == null) {
            producto.setQp(BigDecimal.ZERO);
        }

        return productoDao.save(producto);
    }

    @Override
    public void eliminar(Long id) {
        productoDao.findById(id).ifPresent(producto -> {
            producto.setEstado(Auditoria.ESTADO_ELIMINADO);
            productoDao.save(producto);
        });
    }
}
