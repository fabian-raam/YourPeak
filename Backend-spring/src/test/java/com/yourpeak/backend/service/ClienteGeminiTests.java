package com.yourpeak.backend.service;

import com.sun.net.httpserver.HttpServer;
import com.yourpeak.backend.service.ia.ClienteGemini;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba el cliente contra un servidor HTTP local: nunca llama a la API real de Gemini. */
class ClienteGeminiTests {

    private HttpServer servidor;
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();

    @AfterEach
    void detener() {
        if (servidor != null) servidor.stop(0);
    }

    private String iniciar(int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            ruta.set(intercambio.getRequestURI().getPath());
            clave.set(intercambio.getRequestHeaders().getFirst("x-goog-api-key"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta";
    }

    @Test
    void enviaElPromptConLaClaveYDevuelveElTextoGenerado() throws IOException {
        String url = iniciar(200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"dias\\":"},{"text":"[]}"}]}}]}""");

        Optional<String> texto = new ClienteGemini("clave-de-prueba", "modelo-prueba", url).generarJson("Hola");

        assertEquals(Optional.of("{\"dias\":[]}"), texto);
        assertEquals("/v1beta/models/modelo-prueba:generateContent", ruta.get());
        assertEquals("clave-de-prueba", clave.get());
        assertTrue(cuerpo.get().contains("\"responseMimeType\":\"application/json\""));
        assertTrue(cuerpo.get().contains("Hola"));
    }

    @Test
    void unErrorDeLaApiDevuelveVacio() throws IOException {
        String url = iniciar(429, "{\"error\":{\"message\":\"cuota agotada\"}}");

        assertEquals(Optional.empty(), new ClienteGemini("clave", "modelo", url).generarJson("Hola"));
    }

    @Test
    void sinClaveNoSeConsideraConfigurado() throws IOException {
        String url = iniciar(200, "{}");
        ClienteGemini cliente = new ClienteGemini("  ", "modelo", url);

        assertFalse(cliente.configurado());
        assertEquals(Optional.empty(), cliente.generarJson("Hola"));
        assertEquals(null, ruta.get());
    }
}
