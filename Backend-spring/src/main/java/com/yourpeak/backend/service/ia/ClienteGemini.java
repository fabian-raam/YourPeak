package com.yourpeak.backend.service.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Llama a la API de Gemini y devuelve el texto JSON que genera el modelo.
 * Si la clave no está configurada o la llamada falla, devuelve vacío para que
 * el generador use su respaldo por reglas.
 */
@Component
public class ClienteGemini {

    private static final Logger log = LoggerFactory.getLogger(ClienteGemini.class);

    private final String apiKey;
    private final String modelo;
    private final String urlBase;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = JsonMapper.builder().build();

    public ClienteGemini(
            @Value("${yourpeak.gemini.api-key:}") String apiKey,
            @Value("${yourpeak.gemini.model:gemini-2.5-flash}") String modelo,
            @Value("${yourpeak.gemini.url:https://generativelanguage.googleapis.com/v1beta}") String urlBase
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.modelo = modelo;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    public boolean configurado() {
        return !apiKey.isBlank();
    }

    /** Envía el prompt pidiendo una respuesta JSON y devuelve el texto generado. */
    public Optional<String> generarJson(String prompt) {
        if (!configurado()) return Optional.empty();
        try {
            Map<String, Object> cuerpo = Map.of(
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.4)
            );
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(urlBase + "/models/" + modelo + ":generateContent"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo)))
                    .build();
            HttpResponse<String> respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                // No se registra el cuerpo: podría repetir datos del prompt.
                log.warn("Gemini respondió HTTP {}", respuesta.statusCode());
                return Optional.empty();
            }
            return extraerTexto(json.readValue(respuesta.body(), Map.class));
        } catch (IOException | JacksonException | IllegalArgumentException e) {
            log.warn("No se pudo obtener una respuesta de Gemini: {}", e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    /** La respuesta trae el texto en candidates[0].content.parts[*].text. */
    private static Optional<String> extraerTexto(Map<?, ?> datos) {
        if (!(datos.get("candidates") instanceof List<?> candidatos) || candidatos.isEmpty()) return Optional.empty();
        if (!(candidatos.get(0) instanceof Map<?, ?> candidato)) return Optional.empty();
        if (!(candidato.get("content") instanceof Map<?, ?> contenido)) return Optional.empty();
        if (!(contenido.get("parts") instanceof List<?> partes)) return Optional.empty();
        StringBuilder texto = new StringBuilder();
        for (Object parte : partes) {
            if (parte instanceof Map<?, ?> mapa && mapa.get("text") instanceof String fragmento) texto.append(fragmento);
        }
        return texto.isEmpty() ? Optional.empty() : Optional.of(texto.toString());
    }
}
