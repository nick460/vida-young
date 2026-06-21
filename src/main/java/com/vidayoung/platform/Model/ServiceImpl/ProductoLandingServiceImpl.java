package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Dto.ProductoLandingRequest;
import com.vidayoung.platform.Dto.ProductoLandingResponse;
import com.vidayoung.platform.Dto.ProductoLandingSectionRequest;
import com.vidayoung.platform.Model.Dao.ProductoLandingDao;
import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.ProductoLanding;
import com.vidayoung.platform.Model.Service.ProductoLandingService;
import com.vidayoung.platform.Model.Service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoLandingServiceImpl implements ProductoLandingService {

    private final ProductoService productoService;
    private final ProductoLandingDao productoLandingDao;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoLandingResponse> listar() {
        return productoService.listar().stream()
                .map(producto -> toResponse(producto, productoLandingDao.findByProductoId(producto.getId()).orElse(null)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ProductoLandingResponse> buscarPorProductoId(Long productoId) {
        return productoService.buscarPorId(productoId)
                .map(producto -> toResponse(producto, productoLandingDao.findByProductoId(productoId).orElse(null)));
    }

    @Override
    @Transactional
    public ProductoLandingResponse guardar(Long productoId, ProductoLandingRequest request) {
        Producto producto = productoService.buscarPorId(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        ProductoLanding landing = productoLandingDao.findByProductoId(productoId)
                .orElseGet(() -> ProductoLanding.builder().producto(producto).build());

        landing.setHeadline(clean(request.headline()));
        landing.setSubtitle(clean(request.subtitle()));
        landing.setStory(clean(request.story()));
        landing.setUsage(clean(request.usage()));
        landing.setIngredients(clean(request.ingredients()));
        landing.setBenefits(joinLines(request.benefits()));
        landing.setGallery(joinLines(request.gallery()));
        landing.setSections(writeSections(request.sections()));
        landing.setShareMessage(clean(request.shareMessage()));

        return toResponse(producto, productoLandingDao.save(landing));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String joinLines(List<String> values) {
        if (values == null) {
            return "";
        }

        return String.join("\n", values.stream()
                .map(this::clean)
                .filter(value -> !value.isBlank())
                .toList());
    }

    private ProductoLandingResponse toResponse(Producto producto, ProductoLanding landing) {
        return ProductoLandingResponse.from(producto, landing, readSections(landing));
    }

    private List<ProductoLandingSectionRequest> readSections(ProductoLanding landing) {
        if (landing == null || landing.getSections() == null || landing.getSections().isBlank()) {
            return List.of();
        }

        return landing.getSections().lines()
                .map(this::decodeSection)
                .filter(section -> section != null)
                .toList();
    }

    private String writeSections(List<ProductoLandingSectionRequest> sections) {
        if (sections == null) {
            return "";
        }

        return String.join("\n", sections.stream()
                .filter(section -> section != null)
                .map(this::encodeSection)
                .toList());
    }

    private String encodeSection(ProductoLandingSectionRequest section) {
        return String.join("\t",
                encode(section.type()),
                encode(section.title()),
                encode(section.text()),
                encode(section.imageUrl()),
                encode(joinLines(section.images())),
                encode(section.buttonText()),
                encode(section.whatsappMessage()),
                encode(section.layout())
        );
    }

    private ProductoLandingSectionRequest decodeSection(String line) {
        String[] parts = line.split("\\t", -1);
        if (parts.length < 7) {
            return null;
        }

        return new ProductoLandingSectionRequest(
                decode(parts[0]),
                decode(parts[1]),
                decode(parts[2]),
                decode(parts[3]),
                decode(parts[4]).lines().filter(value -> !value.isBlank()).toList(),
                decode(parts[5]),
                decode(parts[6]),
                parts.length > 7 ? decode(parts[7]) : ""
        );
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().encodeToString(clean(value).getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }
}
