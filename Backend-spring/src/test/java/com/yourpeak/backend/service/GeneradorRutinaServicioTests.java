package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.DiaRutinaResponse;
import com.yourpeak.backend.dto.EjercicioRutinaResponse;
import com.yourpeak.backend.dto.RutinaGeneradaResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.DisponibilidadCliente;
import com.yourpeak.backend.model.Ejercicio;
import com.yourpeak.backend.model.GrupoMuscular;
import com.yourpeak.backend.model.NivelExperiencia;
import com.yourpeak.backend.model.TipoEstado;
import com.yourpeak.backend.model.TipoMedicion;
import com.yourpeak.backend.model.TipoObjetivo;
import com.yourpeak.backend.repository.EjercicioRepository;
import com.yourpeak.backend.repository.NivelActividadRepository;
import com.yourpeak.backend.repository.RegistroFisicoRepository;
import com.yourpeak.backend.repository.TipoObjetivoRepository;
import com.yourpeak.backend.service.ia.ClienteGemini;
import com.yourpeak.backend.service.ia.LimiteGeneracionIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeneradorRutinaServicioTests {

    private final RutinaService rutinaService = mock(RutinaService.class);
    private final EjercicioRepository ejercicios = mock(EjercicioRepository.class);
    private final TipoObjetivoRepository objetivos = mock(TipoObjetivoRepository.class);
    private final NivelActividadRepository actividades = mock(NivelActividadRepository.class);
    private final RegistroFisicoRepository registros = mock(RegistroFisicoRepository.class);
    private final ClienteGemini gemini = mock(ClienteGemini.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC);

    private final Cliente cliente = mock(Cliente.class);
    private final NivelExperiencia principiante = nivel(1, "Principiante");
    private final NivelExperiencia avanzado = nivel(3, "Avanzado");

    // Catálogo de prueba; el id 99 es avanzado y no debe ofrecerse a un principiante.
    private final List<Ejercicio> catalogo = List.of(
            ejercicio(1, "Press de banca", "Pecho", principiante, "REPETICIONES"),
            ejercicio(2, "Remo con barra", "Espalda", principiante, "REPETICIONES"),
            ejercicio(3, "Sentadilla", "Piernas", principiante, "REPETICIONES"),
            ejercicio(4, "Plancha", "Core", principiante, "TIEMPO"),
            ejercicio(5, "Press militar", "Hombros", principiante, "REPETICIONES"),
            ejercicio(6, "Curl de bíceps", "Brazos", principiante, "REPETICIONES"),
            ejercicio(99, "Muscle-up", "Espalda", avanzado, "REPETICIONES")
    );

    private final Map<Integer, DisponibilidadCliente> disponibilidad = new HashMap<>(Map.of(
            1, horario(1, "18:00", "19:15"),
            3, horario(3, "18:00", "19:00")
    ));

    @BeforeEach
    void preparar() {
        when(cliente.getIdCliente()).thenReturn(10L);
        when(cliente.getIdTipoObjetivo()).thenReturn(2);
        when(cliente.getSexo()).thenReturn("MUJER");
        TipoObjetivo objetivo = mock(TipoObjetivo.class);
        when(objetivo.getNombre()).thenReturn("Ganar masa muscular");
        when(objetivos.findById(2)).thenReturn(Optional.of(objetivo));
        when(registros.findFirstByCliente_IdClienteOrderByFechaRegistroDesc(anyLong())).thenReturn(Optional.empty());
        when(rutinaService.buscarClienteActivo(5L)).thenReturn(cliente);
        when(rutinaService.cargarDisponibilidad(cliente)).thenReturn(disponibilidad);
        when(rutinaService.resolverNivel(cliente)).thenReturn(principiante);
        when(ejercicios.findAllByOrderByNombreAsc()).thenReturn(catalogo);
    }

    private GeneradorRutinaService servicio(int limiteDiario) {
        return new GeneradorRutinaService(rutinaService, ejercicios, objetivos, actividades, registros,
                gemini, new LimiteGeneracionIa(limiteDiario, reloj));
    }

    @Test
    void sinIaConfiguradaUsaElGeneradorPorReglas() {
        when(gemini.configurado()).thenReturn(false);

        RutinaGeneradaResponse respuesta = servicio(5).generar(5L);

        assertEquals(GeneradorRutinaService.FUENTE_REGLAS, respuesta.fuente());
        assertTrue(respuesta.mensaje().contains("no está configurada"));
        verify(gemini, never()).generarJson(anyString());
        comprobarRutinaValida(respuesta);
    }

    @Test
    void respuestaDeLaIaSeCorrigeYCompletaConReglas() {
        when(gemini.configurado()).thenReturn(true);
        // Día 1: serie fuera de rango, ejercicio avanzado, ejercicio repetido y medición equivocada.
        // Día 2: no está disponible. Día 3: la IA lo olvidó.
        when(gemini.generarJson(anyString())).thenReturn(Optional.of("""
                ```json
                {"dias":[
                  {"diaSemana":1,"ejercicios":[
                    {"idEjercicio":1,"series":9,"repeticiones":"10","segundos":null,"descansoSegundos":90},
                    {"idEjercicio":99,"series":3,"repeticiones":8,"segundos":null,"descansoSegundos":90},
                    {"idEjercicio":1,"series":3,"repeticiones":8,"segundos":null,"descansoSegundos":90},
                    {"idEjercicio":4,"series":3,"repeticiones":12,"segundos":null,"descansoSegundos":30}
                  ]},
                  {"diaSemana":2,"ejercicios":[{"idEjercicio":2,"series":3,"repeticiones":10,"segundos":null,"descansoSegundos":60}]}
                ]}
                ```"""));

        RutinaGeneradaResponse respuesta = servicio(5).generar(5L);

        assertEquals(GeneradorRutinaService.FUENTE_IA, respuesta.fuente());
        List<EjercicioRutinaResponse> lunes = respuesta.dias().get(0).ejercicios();
        assertEquals(List.of(1, 4), lunes.stream().map(EjercicioRutinaResponse::idEjercicio).toList());
        assertEquals(6, lunes.get(0).series());
        assertEquals(10, lunes.get(0).repeticiones());
        assertNull(lunes.get(1).repeticiones());
        assertEquals(30, lunes.get(1).segundos());
        assertTrue(respuesta.dias().get(1).ejercicios().isEmpty());
        assertFalse(respuesta.dias().get(2).ejercicios().isEmpty());
        comprobarRutinaValida(respuesta);
    }

    @Test
    void elPromptSoloLlevaDatosAnonimosYElCatalogoPermitido() {
        when(gemini.configurado()).thenReturn(true);
        when(gemini.generarJson(anyString())).thenReturn(Optional.empty());

        servicio(5).generar(5L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).generarJson(prompt.capture());
        assertTrue(prompt.getValue().contains("Ganar masa muscular"));
        assertTrue(prompt.getValue().contains("Día 1: 18:00 a 19:15 (75 minutos)"));
        assertTrue(prompt.getValue().contains("- 3 | Sentadilla | Piernas | Principiante | REPETICIONES"));
        assertFalse(prompt.getValue().contains("Muscle-up"));
        verify(cliente, never()).getUsuario();
    }

    @Test
    void respuestaInvalidaDeLaIaUsaReglas() {
        when(gemini.configurado()).thenReturn(true);
        when(gemini.generarJson(anyString())).thenReturn(Optional.of("No puedo ayudarte con eso."));

        RutinaGeneradaResponse respuesta = servicio(5).generar(5L);

        assertEquals(GeneradorRutinaService.FUENTE_REGLAS, respuesta.fuente());
        assertTrue(respuesta.mensaje().contains("no respondió con una rutina válida"));
        comprobarRutinaValida(respuesta);
    }

    @Test
    void superadoElLimiteDiarioNoSeLlamaALaIa() {
        when(gemini.configurado()).thenReturn(true);
        when(gemini.generarJson(anyString())).thenReturn(Optional.empty());
        GeneradorRutinaService generador = servicio(1);

        generador.generar(5L);
        RutinaGeneradaResponse segunda = generador.generar(5L);

        assertEquals(GeneradorRutinaService.FUENTE_REGLAS, segunda.fuente());
        assertTrue(segunda.mensaje().contains("límite diario"));
        verify(gemini).generarJson(anyString());
    }

    @Test
    void sinDiasDisponiblesNoSeGeneraNada() {
        disponibilidad.clear();

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio(5).generar(5L));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void reglasRepartenGruposEnTresDias() {
        when(gemini.configurado()).thenReturn(false);
        disponibilidad.put(5, horario(5, "07:00", "08:00"));

        RutinaGeneradaResponse respuesta = servicio(5).generar(5L);

        // Con tres días se usa empuje, tirón y piernas: el primer ejercicio de cada día es distinto.
        Set<String> primerGrupo = respuesta.dias().stream().filter(DiaRutinaResponse::disponible)
                .map(dia -> dia.ejercicios().get(0).grupoMuscular()).collect(Collectors.toSet());
        assertEquals(Set.of("Pecho", "Espalda", "Piernas"), primerGrupo);
        comprobarRutinaValida(respuesta);
    }

    /** Reglas que cualquier propuesta debe cumplir para que RutinaService la acepte al guardarla. */
    private void comprobarRutinaValida(RutinaGeneradaResponse respuesta) {
        assertEquals(7, respuesta.dias().size());
        Set<Integer> permitidos = Set.of(1, 2, 3, 4, 5, 6);
        for (DiaRutinaResponse dia : respuesta.dias()) {
            DisponibilidadCliente horario = disponibilidad.get(dia.diaSemana());
            assertEquals(horario != null, dia.disponible());
            if (horario == null) {
                assertTrue(dia.ejercicios().isEmpty());
                continue;
            }
            assertFalse(dia.ejercicios().isEmpty(), "Cada día disponible debe tener ejercicios");
            assertTrue(dia.ejercicios().size() <= GeneradorRutinaService.MAX_EJERCICIOS_DIA);
            assertEquals(dia.ejercicios().size(),
                    dia.ejercicios().stream().map(EjercicioRutinaResponse::idEjercicio).distinct().count());
            long segundos = 0;
            for (EjercicioRutinaResponse e : dia.ejercicios()) {
                assertTrue(permitidos.contains(e.idEjercicio()));
                assertEquals("IA", e.origen());
                boolean tiempo = "TIEMPO".equals(e.tipoMedicion());
                assertEquals(tiempo, e.segundos() != null);
                assertEquals(!tiempo, e.repeticiones() != null);
                int trabajo = tiempo ? e.segundos() : e.repeticiones() * 3;
                segundos += (long) e.series() * (trabajo + e.descansoSegundos()) + 60;
            }
            long disponibles = java.time.Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toSeconds();
            assertTrue(dia.ejercicios().size() == 1 || segundos <= disponibles, "La sesión debe caber en el horario");
        }
    }

    private static NivelExperiencia nivel(int id, String nombre) {
        NivelExperiencia nivel = mock(NivelExperiencia.class);
        when(nivel.getIdNivelExperiencia()).thenReturn(id);
        when(nivel.getNombre()).thenReturn(nombre);
        return nivel;
    }

    private static Ejercicio ejercicio(int id, String nombre, String grupo, NivelExperiencia nivel, String medicion) {
        Ejercicio ejercicio = mock(Ejercicio.class);
        GrupoMuscular grupoMuscular = mock(GrupoMuscular.class);
        when(grupoMuscular.getNombre()).thenReturn(grupo);
        TipoMedicion tipoMedicion = mock(TipoMedicion.class);
        when(tipoMedicion.getNombre()).thenReturn(medicion);
        TipoEstado activo = mock(TipoEstado.class);
        when(activo.getNombre()).thenReturn("ACTIVO");
        when(ejercicio.getIdEjercicio()).thenReturn(id);
        when(ejercicio.getNombre()).thenReturn(nombre);
        when(ejercicio.getGrupoMuscular()).thenReturn(grupoMuscular);
        when(ejercicio.getNivelExperiencia()).thenReturn(nivel);
        when(ejercicio.getTipoMedicion()).thenReturn(tipoMedicion);
        when(ejercicio.getTipoEstado()).thenReturn(activo);
        return ejercicio;
    }

    private static DisponibilidadCliente horario(int dia, String inicio, String fin) {
        DisponibilidadCliente horario = mock(DisponibilidadCliente.class);
        when(horario.getDiaSemana()).thenReturn(dia);
        when(horario.getHoraInicio()).thenReturn(LocalTime.parse(inicio));
        when(horario.getHoraFin()).thenReturn(LocalTime.parse(fin));
        return horario;
    }
}
