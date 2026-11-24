package com.yourpeak.backend.service.ia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuenta las generaciones con IA de cada cliente por día para no agotar la cuota
 * gratuita de Gemini. Al pasar el límite se usa el generador por reglas.
 */
@Component
public class LimiteGeneracionIa {

    private final int maximoDiario;
    private final Clock reloj;
    private final Map<Long, Uso> usos = new ConcurrentHashMap<>();

    @Autowired
    public LimiteGeneracionIa(@Value("${yourpeak.ia.limite-diario:5}") int maximoDiario) {
        this(maximoDiario, Clock.system(ZoneId.of("America/Lima")));
    }

    public LimiteGeneracionIa(int maximoDiario, Clock reloj) {
        this.maximoDiario = maximoDiario;
        this.reloj = reloj;
    }

    /** Registra un intento y dice si todavía está dentro del límite diario. */
    public boolean intentar(Long idCliente) {
        LocalDate hoy = LocalDate.now(reloj);
        Uso uso = usos.compute(idCliente, (id, anterior) -> anterior == null || !anterior.fecha().equals(hoy)
                ? new Uso(hoy, 1)
                : new Uso(hoy, anterior.cantidad() + 1));
        return uso.cantidad() <= maximoDiario;
    }

    private record Uso(LocalDate fecha, int cantidad) { }
}
