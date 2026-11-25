package com.yourpeak.backend.service;

import com.yourpeak.backend.dto.DiaRutinaResponse;
import com.yourpeak.backend.dto.EjercicioRutinaResponse;
import com.yourpeak.backend.dto.RutinaGeneradaResponse;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.DisponibilidadCliente;
import com.yourpeak.backend.model.Ejercicio;
import com.yourpeak.backend.model.NivelExperiencia;
import com.yourpeak.backend.model.RegistroFisico;
import com.yourpeak.backend.repository.EjercicioRepository;
import com.yourpeak.backend.repository.NivelActividadRepository;
import com.yourpeak.backend.repository.RegistroFisicoRepository;
import com.yourpeak.backend.repository.TipoObjetivoRepository;
import com.yourpeak.backend.service.ia.ClienteGemini;
import com.yourpeak.backend.service.ia.LimiteGeneracionIa;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Propone una rutina semanal (HU-11). Usa Gemini cuando está configurado y, si no
 * está disponible, falla o responde algo inválido, recurre a un generador por reglas.
 * La propuesta no se guarda: el cliente la revisa y la guarda con {@link RutinaService}.
 */
@Service
public class GeneradorRutinaService {

    public static final String FUENTE_IA = "GEMINI";
    public static final String FUENTE_REGLAS = "REGLAS";
    static final int MAX_EJERCICIOS_DIA = 8;
    private static final int SEGUNDOS_POR_REPETICION = 3;
    private static final int SEGUNDOS_CAMBIO_EJERCICIO = 60;

    // Plantillas de grupos musculares según cuántos días entrena la persona.
    private static final List<String> CUERPO_COMPLETO = List.of("Piernas", "Pecho", "Espalda", "Hombros", "Core", "Brazos", "Cardio");
    private static final List<String> TREN_SUPERIOR = List.of("Pecho", "Espalda", "Hombros", "Brazos", "Core");
    private static final List<String> TREN_INFERIOR = List.of("Piernas", "Core", "Cardio");
    private static final List<String> EMPUJE = List.of("Pecho", "Hombros", "Brazos", "Core");
    private static final List<String> TIRON = List.of("Espalda", "Brazos", "Core");
    private static final List<String> PIERNAS = List.of("Piernas", "Core", "Cardio");

    private final RutinaService rutinaService;
    private final EjercicioRepository ejercicioRepository;
    private final TipoObjetivoRepository objetivoRepository;
    private final NivelActividadRepository actividadRepository;
    private final RegistroFisicoRepository registroRepository;
    private final ClienteGemini gemini;
    private final LimiteGeneracionIa limite;
    private final ObjectMapper json = JsonMapper.builder().build();

    public GeneradorRutinaService(RutinaService rutinaService,
                                  EjercicioRepository ejercicioRepository,
                                  TipoObjetivoRepository objetivoRepository,
                                  NivelActividadRepository actividadRepository,
                                  RegistroFisicoRepository registroRepository,
                                  ClienteGemini gemini,
                                  LimiteGeneracionIa limite) {
        this.rutinaService = rutinaService;
        this.ejercicioRepository = ejercicioRepository;
        this.objetivoRepository = objetivoRepository;
        this.actividadRepository = actividadRepository;
        this.registroRepository = registroRepository;
        this.gemini = gemini;
        this.limite = limite;
    }

    @Transactional(readOnly = true)
    public RutinaGeneradaResponse generar(Long idUsuario) {
        Cliente cliente = rutinaService.buscarClienteActivo(idUsuario);
        Map<Integer, DisponibilidadCliente> disponibilidad = new TreeMap<>(rutinaService.cargarDisponibilidad(cliente));
        if (disponibilidad.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Marca al menos un día disponible antes de generar tu rutina.");
        }
        NivelExperiencia nivel = rutinaService.resolverNivel(cliente);
        List<Ejercicio> catalogo = ejercicioRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> "ACTIVO".equalsIgnoreCase(e.getTipoEstado().getNombre()))
                .filter(e -> e.getNivelExperiencia().getIdNivelExperiencia() <= nivel.getIdNivelExperiencia())
                .toList();
        if (catalogo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay ejercicios activos para tu nivel. Pide al gimnasio que amplíe el catálogo.");
        }

        Perfil perfil = perfil(cliente, nivel);
        Pauta pauta = pauta(perfil);
        Map<Integer, List<Item>> reglas = generarPorReglas(disponibilidad, catalogo, pauta);

        Map<Integer, List<Item>> propuestaIa = null;
        String mensaje;
        if (!gemini.configurado()) {
            mensaje = "La IA no está configurada en el servidor, así que la propuesta se armó con el generador por reglas.";
        } else if (!limite.intentar(cliente.getIdCliente())) {
            mensaje = "Alcanzaste el límite diario de propuestas con IA; esta se armó con el generador por reglas.";
        } else {
            Map<Integer, Ejercicio> porId = catalogo.stream()
                    .collect(Collectors.toMap(Ejercicio::getIdEjercicio, Function.identity()));
            propuestaIa = gemini.generarJson(prompt(perfil, disponibilidad, catalogo))
                    .flatMap(texto -> interpretar(texto, disponibilidad, porId, pauta))
                    .orElse(null);
            mensaje = propuestaIa == null
                    ? "La IA no respondió con una rutina válida, así que se usó el generador por reglas."
                    : "Rutina propuesta con IA (Gemini) a partir de tu perfil, tus horarios y el catálogo del gimnasio.";
        }

        Map<Integer, List<Item>> propuesta = reglas;
        String fuente = FUENTE_REGLAS;
        if (propuestaIa != null) {
            fuente = FUENTE_IA;
            propuesta = new TreeMap<>(propuestaIa);
            // Si la IA dejó vacío algún día disponible, se completa con las reglas.
            for (Integer dia : disponibilidad.keySet()) {
                if (propuesta.getOrDefault(dia, List.of()).isEmpty()) propuesta.put(dia, reglas.get(dia));
            }
        }
        return new RutinaGeneradaResponse(fuente,
                mensaje + " Revísala y pulsa «Guardar rutina» para conservarla.",
                respuesta(propuesta, disponibilidad));
    }

    // ---------- Datos del cliente ----------

    private Perfil perfil(Cliente cliente, NivelExperiencia nivel) {
        String objetivo = cliente.getIdTipoObjetivo() == null ? null
                : objetivoRepository.findById(cliente.getIdTipoObjetivo()).map(o -> o.getNombre()).orElse(null);
        String actividad = cliente.getIdNivelActividad() == null ? null
                : actividadRepository.findById(cliente.getIdNivelActividad()).map(a -> a.getNombre()).orElse(null);
        Optional<RegistroFisico> registro = registroRepository
                .findFirstByCliente_IdClienteOrderByFechaRegistroDesc(cliente.getIdCliente());
        return new Perfil(objetivo, nivel.getNombre(), actividad, cliente.getSexo(),
                registro.map(r -> r.getPesoKg().toPlainString()).orElse(null),
                registro.map(r -> r.getTallaCm().toPlainString()).orElse(null));
    }

    /** Series, repeticiones y descansos de referencia según objetivo y nivel. */
    static Pauta pauta(Perfil perfil) {
        String objetivo = perfil.objetivo() == null ? "" : perfil.objetivo().toLowerCase(Locale.ROOT);
        String nivel = perfil.nivel() == null ? "" : perfil.nivel().toLowerCase(Locale.ROOT);
        boolean principiante = nivel.contains("principiante");
        boolean avanzado = nivel.contains("avanzado");
        int segundos = principiante ? 30 : avanzado ? 60 : 45;
        if (objetivo.contains("perder")) return new Pauta(principiante ? 2 : 3, 15, segundos, 45);
        if (objetivo.contains("ganar") || objetivo.contains("masa")) return new Pauta(principiante ? 3 : 4, 10, segundos, 90);
        return new Pauta(principiante ? 2 : 3, 12, segundos, 60);
    }

    // ---------- Generador por reglas ----------

    Map<Integer, List<Item>> generarPorReglas(Map<Integer, DisponibilidadCliente> disponibilidad,
                                              List<Ejercicio> catalogo, Pauta pauta) {
        Map<String, List<Ejercicio>> porGrupo = catalogo.stream()
                .sorted(Comparator.comparing(Ejercicio::getNombre))
                .collect(Collectors.groupingBy(e -> e.getGrupoMuscular().getNombre().toLowerCase(Locale.ROOT),
                        LinkedHashMap::new, Collectors.toList()));
        List<List<String>> plantillas = plantillas(disponibilidad.size());
        Map<List<String>, Integer> vecesUsada = new HashMap<>();

        Map<Integer, List<Item>> semana = new TreeMap<>();
        int indice = 0;
        for (Map.Entry<Integer, DisponibilidadCliente> entrada : disponibilidad.entrySet()) {
            List<String> plantilla = plantillas.get(indice++ % plantillas.size());
            // Rota la elección cuando la misma plantilla se repite en la semana, para variar ejercicios.
            int rotacion = vecesUsada.merge(plantilla, 1, Integer::sum) - 1;
            List<Ejercicio> candidatos = candidatos(plantilla, porGrupo, catalogo, rotacion);
            semana.put(entrada.getKey(), llenarDia(candidatos, minutos(entrada.getValue()), pauta));
        }
        return semana;
    }

    private static List<List<String>> plantillas(int dias) {
        if (dias <= 2) return List.of(CUERPO_COMPLETO);
        if (dias == 3) return List.of(EMPUJE, TIRON, PIERNAS);
        if (dias == 4) return List.of(TREN_SUPERIOR, TREN_INFERIOR);
        return List.of(EMPUJE, TIRON, PIERNAS, TREN_SUPERIOR, TREN_INFERIOR);
    }

    /** Ordena el catálogo alternando grupos de la plantilla; al final añade el resto como reserva. */
    private static List<Ejercicio> candidatos(List<String> plantilla, Map<String, List<Ejercicio>> porGrupo,
                                              List<Ejercicio> catalogo, int rotacion) {
        List<List<Ejercicio>> colas = new ArrayList<>();
        for (String grupo : plantilla) {
            List<Ejercicio> lista = porGrupo.getOrDefault(grupo.toLowerCase(Locale.ROOT), List.of());
            if (lista.isEmpty()) continue;
            List<Ejercicio> rotada = new ArrayList<>(lista);
            Collections.rotate(rotada, -(rotacion % lista.size()));
            colas.add(rotada);
        }
        List<Ejercicio> orden = new ArrayList<>();
        while (colas.stream().anyMatch(cola -> !cola.isEmpty())) {
            for (List<Ejercicio> cola : colas) if (!cola.isEmpty()) orden.add(cola.removeFirst());
        }
        catalogo.stream().filter(e -> !orden.contains(e)).forEach(orden::add);
        return orden;
    }

    private static List<Item> llenarDia(List<Ejercicio> candidatos, long minutosDisponibles, Pauta pauta) {
        List<Item> dia = new ArrayList<>();
        long segundosUsados = 0;
        for (Ejercicio ejercicio : candidatos) {
            if (dia.size() >= MAX_EJERCICIOS_DIA) break;
            Item item = esTiempo(ejercicio)
                    ? new Item(ejercicio, pauta.series(), null, pauta.segundos(), pauta.descanso())
                    : new Item(ejercicio, pauta.series(), pauta.repeticiones(), null, pauta.descanso());
            long duracion = duracionSegundos(item);
            if (!dia.isEmpty() && segundosUsados + duracion > minutosDisponibles * 60) break;
            dia.add(item);
            segundosUsados += duracion;
        }
        return dia;
    }

    // ---------- Respuesta de la IA ----------

    /** Lee el JSON de la IA y descarta o corrige todo lo que no cumpla las reglas de la rutina. */
    Optional<Map<Integer, List<Item>>> interpretar(String texto, Map<Integer, DisponibilidadCliente> disponibilidad,
                                                   Map<Integer, Ejercicio> catalogo, Pauta pauta) {
        Map<?, ?> datos;
        try {
            datos = json.readValue(sinCercasDeCodigo(texto), Map.class);
        } catch (JacksonException e) {
            return Optional.empty();
        }
        if (!(datos.get("dias") instanceof List<?> dias)) return Optional.empty();

        Map<Integer, List<Item>> semana = new TreeMap<>();
        for (Object elemento : dias) {
            if (!(elemento instanceof Map<?, ?> dia)) continue;
            Integer numeroDia = entero(dia.get("diaSemana"));
            if (numeroDia == null || !disponibilidad.containsKey(numeroDia) || semana.containsKey(numeroDia)) continue;
            if (!(dia.get("ejercicios") instanceof List<?> ejercicios)) continue;

            List<Item> items = new ArrayList<>();
            Set<Integer> usados = new HashSet<>();
            long limiteSegundos = minutos(disponibilidad.get(numeroDia)) * 60;
            long segundosUsados = 0;
            for (Object bruto : ejercicios) {
                if (!(bruto instanceof Map<?, ?> datosEjercicio) || items.size() >= MAX_EJERCICIOS_DIA) continue;
                Integer id = entero(datosEjercicio.get("idEjercicio"));
                Ejercicio ejercicio = id == null ? null : catalogo.get(id);
                if (ejercicio == null || !usados.add(id)) continue;

                int series = acotar(entero(datosEjercicio.get("series")), 1, 6, pauta.series());
                int descanso = acotar(entero(datosEjercicio.get("descansoSegundos")), 0, 300, pauta.descanso());
                Item item = esTiempo(ejercicio)
                        ? new Item(ejercicio, series, null, acotar(entero(datosEjercicio.get("segundos")), 10, 300, pauta.segundos()), descanso)
                        : new Item(ejercicio, series, acotar(entero(datosEjercicio.get("repeticiones")), 1, 30, pauta.repeticiones()), null, descanso);
                long duracion = duracionSegundos(item);
                if (!items.isEmpty() && segundosUsados + duracion > limiteSegundos) break;
                items.add(item);
                segundosUsados += duracion;
            }
            semana.put(numeroDia, items);
        }
        boolean tieneEjercicios = semana.values().stream().anyMatch(items -> !items.isEmpty());
        return tieneEjercicios ? Optional.of(semana) : Optional.empty();
    }

    /** Solo datos anónimos: nada de nombre, correo ni identificadores del cliente. */
    String prompt(Perfil perfil, Map<Integer, DisponibilidadCliente> disponibilidad, List<Ejercicio> catalogo) {
        StringBuilder texto = new StringBuilder("""
                Eres un entrenador personal. Diseña la rutina semanal de gimnasio de un cliente.

                Perfil del cliente:
                """);
        texto.append("- Objetivo: ").append(valor(perfil.objetivo())).append('\n')
                .append("- Nivel de experiencia: ").append(valor(perfil.nivel())).append('\n')
                .append("- Nivel de actividad: ").append(valor(perfil.actividad())).append('\n')
                .append("- Sexo: ").append(valor(perfil.sexo())).append('\n')
                .append("- Peso: ").append(perfil.peso() == null ? "sin dato" : perfil.peso() + " kg").append('\n')
                .append("- Talla: ").append(perfil.talla() == null ? "sin dato" : perfil.talla() + " cm").append("\n\n")
                .append("Días disponibles (1 = lunes ... 7 = domingo) y su duración máxima:\n");
        disponibilidad.forEach((dia, horario) -> texto.append("- Día ").append(dia).append(": ")
                .append(horario.getHoraInicio()).append(" a ").append(horario.getHoraFin())
                .append(" (").append(minutos(horario)).append(" minutos)\n"));
        texto.append("\nCatálogo permitido (id | nombre | grupo muscular | nivel | medición):\n");
        catalogo.forEach(e -> texto.append("- ").append(e.getIdEjercicio()).append(" | ").append(e.getNombre())
                .append(" | ").append(e.getGrupoMuscular().getNombre()).append(" | ")
                .append(e.getNivelExperiencia().getNombre()).append(" | ")
                .append(e.getTipoMedicion().getNombre().toUpperCase(Locale.ROOT)).append('\n'));
        texto.append("""

                Reglas:
                - Programa ejercicios solo en los días disponibles y que quepan en su duración.
                - Usa únicamente ids del catálogo y no repitas un ejercicio dentro del mismo día.
                - Reparte los grupos musculares en la semana y evita trabajar el mismo grupo principal en días seguidos.
                - Si la medición es REPETICIONES, indica "repeticiones" y deja "segundos" en null.
                  Si es TIEMPO, indica "segundos" y deja "repeticiones" en null.
                - Entre 2 y 8 ejercicios por día, series entre 1 y 6, descanso entre 0 y 300 segundos.
                - Ajusta series, repeticiones y descansos al objetivo y al nivel del cliente.

                Responde solo con JSON con esta forma, sin texto adicional:
                {"dias":[{"diaSemana":1,"ejercicios":[{"idEjercicio":3,"series":3,"repeticiones":12,"segundos":null,"descansoSegundos":60}]}]}
                """);
        return texto.toString();
    }

    // ---------- Utilidades ----------

    private List<DiaRutinaResponse> respuesta(Map<Integer, List<Item>> propuesta,
                                              Map<Integer, DisponibilidadCliente> disponibilidad) {
        List<DiaRutinaResponse> dias = new ArrayList<>(7);
        for (int numeroDia = 1; numeroDia <= 7; numeroDia++) {
            DisponibilidadCliente horario = disponibilidad.get(numeroDia);
            List<EjercicioRutinaResponse> ejercicios = new ArrayList<>();
            if (horario != null) {
                int orden = 1;
                for (Item item : propuesta.getOrDefault(numeroDia, List.of())) {
                    Ejercicio e = item.ejercicio();
                    // La propuesta aún no está guardada en la base de datos, por eso no tiene idPlanEjercicio.
                    ejercicios.add(new EjercicioRutinaResponse(null, e.getIdEjercicio(), e.getNombre(),
                            e.getGrupoMuscular().getNombre(), e.getTipoMedicion().getNombre(), orden++,
                            item.series(), item.repeticiones(), item.segundos(), item.descanso(), "IA",
                            e.getImagenRuta()));
                }
            }
            dias.add(new DiaRutinaResponse(numeroDia, horario != null,
                    horario == null ? null : horario.getHoraInicio(),
                    horario == null ? null : horario.getHoraFin(), ejercicios));
        }
        return dias;
    }

    private static long minutos(DisponibilidadCliente horario) {
        return Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toMinutes();
    }

    private static boolean esTiempo(Ejercicio ejercicio) {
        return "TIEMPO".equalsIgnoreCase(ejercicio.getTipoMedicion().getNombre());
    }

    /** Estimación del tiempo de un ejercicio: trabajo y descanso por serie, más el cambio de máquina. */
    static long duracionSegundos(Item item) {
        int trabajo = item.segundos() != null ? item.segundos() : item.repeticiones() * SEGUNDOS_POR_REPETICION;
        return (long) item.series() * (trabajo + item.descanso()) + SEGUNDOS_CAMBIO_EJERCICIO;
    }

    private static String sinCercasDeCodigo(String texto) {
        String limpio = texto.strip();
        if (limpio.startsWith("```")) {
            int salto = limpio.indexOf('\n');
            limpio = salto < 0 ? "" : limpio.substring(salto + 1);
            if (limpio.endsWith("```")) limpio = limpio.substring(0, limpio.length() - 3);
        }
        return limpio.strip();
    }

    private static Integer entero(Object valor) {
        if (valor instanceof Number numero) return (int) Math.round(numero.doubleValue());
        if (valor instanceof String texto) {
            try {
                return (int) Math.round(Double.parseDouble(texto.strip()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static int acotar(Integer valor, int minimo, int maximo, int porDefecto) {
        if (valor == null) return porDefecto;
        return Math.max(minimo, Math.min(maximo, valor));
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "sin dato" : texto;
    }

    record Perfil(String objetivo, String nivel, String actividad, String sexo, String peso, String talla) { }

    record Pauta(int series, int repeticiones, int segundos, int descanso) { }

    record Item(Ejercicio ejercicio, int series, Integer repeticiones, Integer segundos, int descanso) { }
}
