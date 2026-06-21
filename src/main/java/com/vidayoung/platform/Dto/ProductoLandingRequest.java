package com.vidayoung.platform.Dto;

import java.util.List;

public record ProductoLandingRequest(
        String headline,
        String subtitle,
        String story,
        String usage,
        String ingredients,
        List<String> benefits,
        List<String> gallery,
        List<ProductoLandingSectionRequest> sections,
        String shareMessage
) {
}
