package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.ProductoCategoriaDao;
import com.vidayoung.platform.Model.Dao.ProductoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.ProductoCategoria;
import com.vidayoung.platform.Model.Service.ProductoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductoServiceIplem implements ProductoService {

    private final ProductoDao productoDao;
    private final ProductoCategoriaDao productoCategoriaDao;

    @Override
    public List<Producto> listar() {
        return productoDao.findAll().stream()
                .filter(producto -> Auditoria.ESTADO_ACTIVO.equals(producto.getEstado()))
                .toList();
    }

    @Override
    public List<Producto> listarParaShop() {
        return listar().stream()
                .filter(producto -> Boolean.TRUE.equals(producto.getListarEnShop()))
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
        boolean isNew = producto.getId() == null;
        Producto existing = isNew ? null : productoDao.findById(producto.getId()).orElse(null);
        ProductoCategoria categoria = resolveCategoria(producto.getCategoria());

        producto.setCategoria(categoria.getNombre());

        if (isNew) {
            producto.setSku(nextSku(categoria.getSigla()));
        } else if (existing != null) {
            producto.setSku(existing.getSku());
        }

        if (producto.getPv() == null) {
            producto.setPv(BigDecimal.ZERO);
        }

        if (producto.getQp() == null) {
            producto.setQp(BigDecimal.ZERO);
        }

        if (producto.getCr() == null) {
            producto.setCr(BigDecimal.ZERO);
        }

        if (producto.getListarEnShop() == null) {
            producto.setListarEnShop(Boolean.FALSE);
        }

        return productoDao.save(producto);
    }

    private ProductoCategoria resolveCategoria(String categoria) {
        String nombre = categoria == null ? "" : categoria.trim();
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("Selecciona una categoria registrada.");
        }

        return productoCategoriaDao.findByNombreIgnoreCase(nombre)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("La categoria seleccionada no existe o no esta activa."));
    }

    private String nextSku(String sigla) {
        String prefix = sigla.toLowerCase(Locale.ROOT);
        int next = productoDao.findTopBySkuStartingWithIgnoreCaseOrderBySkuDesc(prefix + "-")
                .map(Producto::getSku)
                .map(this::extractSkuNumber)
                .orElse(0) + 1;

        return "%s-%03d".formatted(prefix, next);
    }

    private int extractSkuNumber(String sku) {
        if (sku == null || !sku.contains("-")) {
            return 0;
        }

        String suffix = sku.substring(sku.lastIndexOf('-') + 1);
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    public void eliminar(Long id) {
        productoDao.findById(id).ifPresent(producto -> {
            producto.setEstado(Auditoria.ESTADO_ELIMINADO);
            productoDao.save(producto);
        });
    }
}
