package com.vidayoung.platform.Dto;

import com.vidayoung.platform.Model.Entity.Producto;
import com.vidayoung.platform.Model.Entity.ProductoLanding;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record ProductoLandingResponse(
        Long id,
        Producto producto,
        String headline,
        String subtitle,
        String story,
        String usage,
        String ingredients,
        List<String> benefits,
        List<String> gallery,
        List<ProductoLandingSectionRequest> sections,
        String shareMessage,
        LocalDateTime updatedAt
) {

    public static ProductoLandingResponse from(
            Producto producto,
            ProductoLanding landing,
            List<ProductoLandingSectionRequest> sections
    ) {
        return new ProductoLandingResponse(
                landing == null ? null : landing.getId(),
                producto,
                landing == null ? "" : landing.getHeadline(),
                landing == null ? "" : landing.getSubtitle(),
                landing == null ? "" : landing.getStory(),
                landing == null ? "" : landing.getUsage(),
                landing == null ? "" : landing.getIngredients(),
                lines(landing == null ? "" : landing.getBenefits()),
                lines(landing == null ? "" : landing.getGallery()),
                sections,
                landing == null ? "" : landing.getShareMessage(),
                landing == null ? null : landing.getFechaModificacion()
        );
    }

    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
