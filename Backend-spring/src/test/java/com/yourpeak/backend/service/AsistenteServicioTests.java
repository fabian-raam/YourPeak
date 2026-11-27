package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.AsistenteRequest;
import com.yourpeak.backend.dto.PerfilInicialResponse;
import com.yourpeak.backend.service.ia.ClienteGemini;
import com.yourpeak.backend.service.ia.LimiteChatIa;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenteServicioTests {

    private final ClienteGemini gemini = mock(ClienteGemini.class);
    private final PerfilClienteService perfiles = mock(PerfilClienteService.class);
    private final LimiteChatIa limite = new LimiteChatIa(2, Clock.fixed(Instant.parse("2026-11-27T15:00:00Z"), ZoneId.of("America/Lima")));
    private final AsistenteService servicio = new AsistenteService(gemini, limite, perfiles);
    private final PerfilInicialResponse perfil = new PerfilInicialResponse(true, 1, "Ganar masa muscular", 1, "Principiante",
            2, "Moderado", "F", new BigDecimal("61.50"), new BigDecimal("165.00"), null, null);

    {
        when(perfiles.consultar(7L)).thenReturn(perfil);
        when(gemini.configurado()).thenReturn(true);
    }

    private AsistenteRequest pregunta(String texto) {
        return new AsistenteRequest(texto, List.of());
    }

    @Test
    void respondePreguntasDeAlimentacion() {
        when(gemini.conversarJson(anyString(), any())).thenReturn(Optional.of(
                "{\"enTema\": true, \"respuesta\": \"Prueba arroz con pollo y verduras.\"}"));

        var respuesta = servicio.responder(7L, pregunta("¿Qué ceno después de entrenar?"));

        assertTrue(respuesta.enTema());
        assertEquals("Prueba arroz con pollo y verduras.", respuesta.respuesta());
        assertEquals(1, respuesta.consultasRestantesHoy());
    }

    @Test
    void fueraDeTemaSiempreMuestraElMensajeFijo() {
        when(gemini.conversarJson(anyString(), any())).thenReturn(Optional.of(
                "{\"enTema\": false, \"respuesta\": \"La capital de Francia es París.\"}"));

        var respuesta = servicio.responder(7L, pregunta("¿Cuál es la capital de Francia?"));

        assertFalse(respuesta.enTema());
        assertEquals(AsistenteService.FUERA_DE_TEMA, respuesta.respuesta());
    }

    @Test
    void elLimiteDiarioCortaLasConsultasSinLlamarAGemini() {
        when(gemini.conversarJson(anyString(), any())).thenReturn(Optional.of("{\"enTema\": true, \"respuesta\": \"Ok\"}"));
        servicio.responder(7L, pregunta("uno"));
        servicio.responder(7L, pregunta("dos"));

        var error = assertThrows(ResponseStatusException.class, () -> servicio.responder(7L, pregunta("tres")));

        assertEquals(429, error.getStatusCode().value());
        verify(gemini, org.mockito.Mockito.times(2)).conversarJson(anyString(), any());
    }

    @Test
    void unFalloDeGeminiNoGastaLaConsulta() {
        when(gemini.conversarJson(anyString(), any())).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class, () -> servicio.responder(7L, pregunta("hola")));

        assertEquals(503, error.getStatusCode().value());
        assertEquals(2, limite.restantes(7L));
    }

    @Test
    void sinClaveNoLlamaAGemini() {
        when(gemini.configurado()).thenReturn(false);

        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> servicio.responder(7L, pregunta("hola"))).getStatusCode().value());
        verify(gemini, never()).conversarJson(anyString(), any());
    }

    @Test
    void elHistorialSeRecortaYUsaLosRolesDeGemini() {
        List<AsistenteRequest.Mensaje> historial = new ArrayList<>();
        for (int i = 0; i < 12; i++) historial.add(new AsistenteRequest.Mensaje(i % 2 == 0 ? "usuario" : "asistente", "m" + i));

        var contenidos = AsistenteService.contenidos(new AsistenteRequest("nueva", historial));

        assertEquals(11, contenidos.size());
        assertEquals("user", contenidos.get(0).get("role"));
        assertEquals("model", contenidos.get(1).get("role"));
        assertEquals("user", contenidos.get(10).get("role"));
    }

    @Test
    void lasInstruccionesIncluyenElPerfilYElAlcance() {
        String instrucciones = AsistenteService.instrucciones(perfil);

        assertTrue(instrucciones.contains("Objetivo: Ganar masa muscular"));
        assertTrue(instrucciones.contains("Peso: 61.5 kg"));
        assertTrue(instrucciones.contains("SOLO respondes sobre alimentación"));
    }
}
