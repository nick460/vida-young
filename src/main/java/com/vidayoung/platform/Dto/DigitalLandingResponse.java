package com.vidayoung.platform.Dto;

import com.vidayoung.platform.Model.Entity.DigitalLanding;
import java.time.LocalDateTime;
import java.util.List;

public record DigitalLandingResponse(
        Long id,
        String slug,
        String title,
        String category,
        String imageUrl,
        String description,
        List<ProductoLandingSectionRequest> sections,
        String shareMessage,
        LocalDateTime updatedAt
) {

    public static DigitalLandingResponse from(DigitalLanding landing, List<ProductoLandingSectionRequest> sections) {
        return new DigitalLandingResponse(
                landing.getId(),
                landing.getSlug(),
                landing.getTitle(),
                landing.getCategory(),
                landing.getImageUrl(),
                landing.getDescription(),
                sections,
                landing.getShareMessage(),
                landing.getFechaModificacion()
        );
    }
}
