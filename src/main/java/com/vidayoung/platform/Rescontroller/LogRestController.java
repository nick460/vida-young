package com.vidayoung.platform.Rescontroller;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class LogRestController {

    private static final Path LOG_DIR = Path.of("logs").toAbsolutePath().normalize();
    private static final Pattern LOG_FILE_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+\\.txt");
    private static final int DEFAULT_LINES = 600;
    private static final int MAX_LINES = 5000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @GetMapping
    public ResponseEntity<List<LogFileResponse>> listarArchivos() throws IOException {
        if (!Files.isDirectory(LOG_DIR)) {
            return ResponseEntity.ok(List.of());
        }

        try (Stream<Path> paths = Files.list(LOG_DIR)) {
            List<LogFileResponse> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> LOG_FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                    .map(this::toResponse)
                    .sorted(Comparator.comparing(LogFileResponse::getLastModified).reversed())
                    .toList();

            return ResponseEntity.ok(files);
        }
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<LogContentResponse> leerArchivo(
            @PathVariable String fileName,
            @RequestParam(defaultValue = "" + DEFAULT_LINES) int lines
    ) throws IOException {
        Path logFile = resolveLogFile(fileName);
        int requestedLines = Math.max(1, Math.min(lines, MAX_LINES));

        if (!Files.exists(logFile) || !Files.isRegularFile(logFile)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new LogContentResponse(
                logFile.getFileName().toString(),
                Files.size(logFile),
                modifiedAt(logFile),
                tail(logFile, requestedLines),
                requestedLines
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    private LogFileResponse toResponse(Path path) {
        try {
            return new LogFileResponse(path.getFileName().toString(), Files.size(path), modifiedAt(path));
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo de log: " + path.getFileName());
        }
    }

    private Path resolveLogFile(String fileName) {
        if (fileName == null || !LOG_FILE_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Nombre de archivo invalido.");
        }

        Path resolved = LOG_DIR.resolve(fileName).normalize();
        if (!resolved.startsWith(LOG_DIR)) {
            throw new IllegalArgumentException("Nombre de archivo invalido.");
        }

        return resolved;
    }

    private String modifiedAt(Path path) throws IOException {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis())
                .atZone(ZoneId.systemDefault()));
    }

    private String tail(Path path, int lines) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long pointer = file.length() - 1;
            int lineCount = 0;

            while (pointer >= 0 && lineCount < lines) {
                file.seek(pointer);
                if (file.read() == '\n') {
                    lineCount++;
                }
                pointer--;
            }

            long start = Math.max(0, pointer + 2);
            byte[] bytes = new byte[(int) (file.length() - start)];
            file.seek(start);
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class LogFileResponse {

        private final String name;
        private final long size;
        private final String lastModified;
    }

    @Getter
    @RequiredArgsConstructor
    public static class LogContentResponse {

        private final String name;
        private final long size;
        private final String lastModified;
        private final String content;
        private final int lines;
    }
}
