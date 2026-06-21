package com.vidayoung.platform.Dto;

import java.util.List;

public record ProductoLandingSectionRequest(
        String type,
        String title,
        String text,
        String imageUrl,
        List<String> images,
        String buttonText,
        String whatsappMessage,
        String layout
) {
}
