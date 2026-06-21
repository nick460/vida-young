package com.vidayoung.platform.Dto;

import java.util.List;

public record DigitalLandingRequest(
        String slug,
        String title,
        String category,
        String imageUrl,
        String description,
        List<ProductoLandingSectionRequest> sections,
        String shareMessage
) {
}
