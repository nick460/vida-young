package com.vidayoung.platform.Rescontroller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadRestController {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Path LANDING_UPLOAD_DIR = Paths.get("uploads", "landings");

    @PostMapping("/landings")
    public ResponseEntity<Map<String, String>> subirImagenLanding(
            @RequestPart("imagen") MultipartFile imagen
    ) throws IOException {
        if (imagen.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = imagen.getContentType();
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        Files.createDirectories(LANDING_UPLOAD_DIR);
        String extension = StringUtils.getFilenameExtension(imagen.getOriginalFilename());
        String safeExtension = extension == null ? "jpg" : extension.toLowerCase(Locale.ROOT);
        String fileName = "landing-" + UUID.randomUUID() + "." + safeExtension;
        Path destino = LANDING_UPLOAD_DIR.resolve(fileName).normalize();
        imagen.transferTo(destino);

        return ResponseEntity.ok(Map.of("url", "/uploads/landings/" + fileName));
    }
}
