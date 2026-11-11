package com.vidayoung.platform.util;

import com.luciad.imageio.webp.WebPWriteParam;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.web.multipart.MultipartFile;

public final class ImageWebpUtil {

    private static final float CALIDAD_WEBP = 0.92f;

    private ImageWebpUtil() {
    }

    public static boolean isImageContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }

    public static void saveAsWebp(MultipartFile file, Path destino) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("La imagen está vacía.");
        }

        String contentType = file.getContentType();
        if (contentType != null && contentType.equalsIgnoreCase("image/webp")) {
            file.transferTo(destino);
            return;
        }

        try (InputStream in = file.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("No se pudo leer la imagen para convertirla a WebP.");
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
            if (!writers.hasNext()) {
                throw new IOException("No se encontró un writer de WebP disponible.");
            }

            ImageWriter writer = writers.next();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(destino.toFile())) {
                writer.setOutput(output);
                WebPWriteParam param = new WebPWriteParam(Locale.getDefault());
                param.setCompressionQuality(CALIDAD_WEBP);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        }
    }
}
