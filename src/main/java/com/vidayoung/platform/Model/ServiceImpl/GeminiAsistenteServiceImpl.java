package com.vidayoung.platform.Model.ServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vidayoung.platform.Dto.Asistente.AsistenteChatRequest;
import com.vidayoung.platform.Dto.Asistente.AsistenteChatResponse;
import com.vidayoung.platform.Dto.Asistente.AsistenteMessage;
import com.vidayoung.platform.Model.Service.AsistenteService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiAsistenteServiceImpl implements AsistenteService {

    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Value("${gemini.api-key:${gemini.api.key:}}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String model;

    @Override
    public AsistenteChatResponse enviarMensaje(AsistenteChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();

        if (message.isBlank()) {
            throw new IllegalArgumentException("El mensaje es obligatorio.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("La API key de Gemini no esta configurada. Define gemini.api-key o la variable GEMINI_API_KEY.");
        }

        try {
            String body = construirBody(request, message);
            String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_ENDPOINT.formatted(encodedModel)))
                    .timeout(Duration.ofSeconds(45))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gemini respondio con error: " + response.body());
            }

            return new AsistenteChatResponse(extraerTexto(response.body()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo conectar con Gemini.", exception);
        }
    }

    private String construirBody(AsistenteChatRequest request, String message) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");

        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(this::mensajeValido)
                    .limit(20)
                    .forEach(item -> agregarContent(contents, normalizarRole(item.getRole()), item.getText().trim()));
        }

        agregarContent(contents, "user", message);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 2048);

        return objectMapper.writeValueAsString(root);
    }

    private boolean mensajeValido(AsistenteMessage message) {
        return message != null && message.getText() != null && !message.getText().trim().isBlank();
    }

    private void agregarContent(ArrayNode contents, String role, String text) {
        ObjectNode content = contents.addObject();
        content.put("role", role);
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", text);
    }

    private String normalizarRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        return "model".equals(normalized) || "assistant".equals(normalized) ? "model" : "user";
    }

    private String extraerTexto(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        StringBuilder builder = new StringBuilder();

        if (parts.isArray()) {
            parts.forEach(part -> {
                String text = part.path("text").asText("");
                if (!text.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append("\n");
                    }
                    builder.append(text);
                }
            });
        }

        String text = builder.toString().trim();
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini no devolvio texto.");
        }

        return text;
    }
}
