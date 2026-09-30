package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.EvolutionApiConfigDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.EvolutionApiConfig;
import com.vidayoung.platform.Model.Service.EvolutionApiService;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvolutionApiServiceImpl implements EvolutionApiService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiServiceImpl.class);
    private static final Long CONFIG_ID = 1L;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static final List<String> MENSAJES = List.of(
            "Hola {nombre}, bienvenido/a a Vida Young. Tu acceso ya está listo:\nUsuario: {usuario}\nContraseña: {password}\nIngresa aquí: {login}\nNos alegra tenerte en la familia Vida Young.",
            "¡Bienvenido/a, {nombre}! Tu cuenta Vida Young fue creada correctamente.\nUsuario: {usuario}\nContraseña: {password}\nAcceso: {login}\nQue este sea el inicio de una gran etapa.",
            "Hola {nombre}, ya formas parte de Vida Young. Estos son tus datos de ingreso:\nUsuario: {usuario}\nContraseña: {password}\nLink: {login}\nEstamos felices de acompañarte en este camino.",
            "{nombre}, te damos la bienvenida a Vida Young. Tu usuario ya está activo:\nUsuario: {usuario}\nContraseña: {password}\nEntra desde: {login}\nMuchos éxitos en esta nueva experiencia.",
            "¡Qué alegría tenerte con nosotros, {nombre}!\nTu acceso a Vida Young es:\nUsuario: {usuario}\nContraseña: {password}\nLogin: {login}\nBienvenido/a al equipo.",
            "Hola {nombre}, tu registro en Vida Young fue aprobado.\nUsuario: {usuario}\nContraseña: {password}\nAccede aquí: {login}\nTe deseamos un excelente inicio.",
            "Bienvenido/a a Vida Young, {nombre}. Ya puedes ingresar con estos datos:\nUsuario: {usuario}\nContraseña: {password}\n{login}\nEstamos listos para crecer contigo.",
            "{nombre}, tu cuenta Vida Young ya está disponible.\nUsuario: {usuario}\nContraseña: {password}\nIngresa en: {login}\nGracias por confiar en nosotros.",
            "Hola {nombre}. Desde hoy eres parte de Vida Young.\nDatos de acceso:\nUsuario: {usuario}\nContraseña: {password}\nLink: {login}\nBienvenido/a y mucho éxito.",
            "¡Bienvenido/a, {nombre}! Tu acceso fue creado con éxito.\nUsuario: {usuario}\nContraseña: {password}\nPlataforma: {login}\nNos alegra que seas parte de Vida Young."
    );

    private final EvolutionApiConfigDao configDao;

    @Override
    public EvolutionApiConfig obtenerConfig() {
        return configDao.findById(CONFIG_ID).orElseGet(() -> configDao.save(EvolutionApiConfig.builder()
                .id(CONFIG_ID)
                .apiUrl("https://evolution.rubina-solutions.tech")
                .loginUrl("https://vidayoung.online/login")
                .codigoPais("591")
                .habilitado(false)
                .build()));
    }

    @Override
    @Transactional
    public EvolutionApiConfig guardarConfig(EvolutionApiConfig config) {
        EvolutionApiConfig actual = obtenerConfig();
        actual.setHabilitado(Boolean.TRUE.equals(config.getHabilitado()));
        actual.setApiUrl(normalizarSinBarra(config.getApiUrl()));
        actual.setApiKey(normalizar(config.getApiKey()));
        actual.setInstanceName(normalizar(config.getInstanceName()));
        actual.setCodigoPais(normalizar(config.getCodigoPais()) == null ? "591" : normalizar(config.getCodigoPais()));
        actual.setLoginUrl(normalizar(config.getLoginUrl()) == null ? "https://vidayoung.online/login" : normalizar(config.getLoginUrl()));
        actual.setEstado(Auditoria.ESTADO_ACTIVO);
        return configDao.save(actual);
    }

    @Override
    @Transactional
    public boolean enviarBienvenida(String nombres, String telefono, String username, String passwordPlano) {
        EvolutionApiConfig config = obtenerConfig();
        if (!Boolean.TRUE.equals(config.getHabilitado())) {
            return false;
        }
        if (normalizar(config.getApiUrl()) == null || normalizar(config.getApiKey()) == null
                || normalizar(config.getInstanceName()) == null || normalizar(telefono) == null) {
            log.warn("Evolution API no configurada o teléfono vacío; bienvenida no enviada.");
            return false;
        }

        int indice = siguienteIndice(config.getUltimoTemplate());
        config.setUltimoTemplate(indice);
        configDao.save(config);

        String texto = MENSAJES.get(indice)
                .replace("{nombre}", valor(nombres, ""))
                .replace("{usuario}", valor(username, ""))
                .replace("{password}", valor(passwordPlano, ""))
                .replace("{login}", valor(config.getLoginUrl(), "https://vidayoung.online/login"));

        String body = "{\"number\":\"" + escapeJson(normalizarTelefono(telefono, config.getCodigoPais()))
                + "\",\"text\":\"" + escapeJson(texto) + "\"}";
        String url = normalizarSinBarra(config.getApiUrl()) + "/message/sendText/" + config.getInstanceName();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("apikey", config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            log.warn("Evolution API respondió {}: {}", response.statusCode(), response.body());
        } catch (Exception exception) {
            log.warn("No se pudo enviar bienvenida por Evolution API: {}", exception.getMessage());
        }
        return false;
    }

    private int siguienteIndice(Integer ultimo) {
        int last = ultimo == null ? -1 : ultimo;
        return Math.floorMod(last + 1, MENSAJES.size());
    }

    private String normalizarTelefono(String telefono, String codigoPais) {
        String digits = telefono == null ? "" : telefono.replaceAll("\\D+", "");
        String code = codigoPais == null ? "591" : codigoPais.replaceAll("\\D+", "");
        if (!code.isBlank() && !digits.startsWith(code)) {
            digits = code + digits;
        }
        return digits;
    }

    private String normalizar(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String normalizarSinBarra(String value) {
        String normalized = normalizar(value);
        return normalized == null ? null : normalized.replaceAll("/+$", "");
    }

    private String valor(String value, String fallback) {
        String normalized = normalizar(value);
        return normalized == null ? fallback : normalized;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
