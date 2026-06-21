package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Dto.DigitalLandingRequest;
import com.vidayoung.platform.Dto.DigitalLandingResponse;
import com.vidayoung.platform.Dto.ProductoLandingSectionRequest;
import com.vidayoung.platform.Model.Dao.DigitalLandingDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.DigitalLanding;
import com.vidayoung.platform.Model.Service.DigitalLandingService;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DigitalLandingServiceImpl implements DigitalLandingService {

    private final DigitalLandingDao digitalLandingDao;

    @Override
    @Transactional(readOnly = true)
    public List<DigitalLandingResponse> listar() {
        return digitalLandingDao.findAll().stream()
                .filter(landing -> Auditoria.ESTADO_ACTIVO.equals(landing.getEstado()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DigitalLandingResponse> buscarPorId(Long id) {
        return digitalLandingDao.findById(id)
                .filter(landing -> Auditoria.ESTADO_ACTIVO.equals(landing.getEstado()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DigitalLandingResponse> buscarPorSlug(String slug) {
        return digitalLandingDao.findBySlug(cleanSlug(slug))
                .filter(landing -> Auditoria.ESTADO_ACTIVO.equals(landing.getEstado()))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public DigitalLandingResponse guardar(DigitalLandingRequest request) {
        DigitalLanding landing = DigitalLanding.builder().build();
        return saveLanding(landing, request);
    }

    @Override
    @Transactional
    public DigitalLandingResponse actualizar(Long id, DigitalLandingRequest request) {
        DigitalLanding landing = digitalLandingDao.findById(id)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new EntityNotFoundException("Landing digital no encontrada"));

        return saveLanding(landing, request);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        digitalLandingDao.findById(id).ifPresent(landing -> {
            landing.setEstado(Auditoria.ESTADO_ELIMINADO);
            digitalLandingDao.save(landing);
        });
    }

    private DigitalLandingResponse saveLanding(DigitalLanding landing, DigitalLandingRequest request) {
        String slug = cleanSlug(request.slug());
        if (slug.isBlank()) {
            slug = cleanSlug(request.title());
        }

        if (slug.isBlank()) {
            throw new IllegalArgumentException("El slug o titulo es obligatorio.");
        }

        Long landingId = landing.getId() == null ? 0L : landing.getId();
        if (digitalLandingDao.existsBySlugAndIdNot(slug, landingId)) {
            throw new IllegalArgumentException("Ya existe una landing con ese slug.");
        }

        landing.setSlug(slug);
        landing.setTitle(required(request.title(), "El titulo es obligatorio."));
        landing.setCategory(clean(request.category()));
        landing.setImageUrl(clean(request.imageUrl()));
        landing.setDescription(clean(request.description()));
        landing.setSections(writeSections(request.sections()));
        landing.setShareMessage(clean(request.shareMessage()));
        landing.setEstado(Auditoria.ESTADO_ACTIVO);

        return toResponse(digitalLandingDao.save(landing));
    }

    private DigitalLandingResponse toResponse(DigitalLanding landing) {
        return DigitalLandingResponse.from(landing, readSections(landing));
    }

    private String required(String value, String message) {
        String cleanValue = clean(value);
        if (cleanValue.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return cleanValue;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanSlug(String value) {
        return clean(value)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
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

    private List<ProductoLandingSectionRequest> readSections(DigitalLanding landing) {
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
