package com.yourpeak.backend.service.ia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Consultas diarias al asistente por cliente, para no agotar la cuota de Gemini. */
@Component
public class LimiteChatIa {

    private final int maximoDiario;
    private final Clock reloj;
    private final Map<Long, Uso> usos = new ConcurrentHashMap<>();

    @Autowired
    public LimiteChatIa(@Value("${yourpeak.ia.chat-limite-diario:30}") int maximoDiario) {
        this(maximoDiario, Clock.system(ZoneId.of("America/Lima")));
    }

    public LimiteChatIa(int maximoDiario, Clock reloj) {
        this.maximoDiario = maximoDiario;
        this.reloj = reloj;
    }

    public int maximoDiario() {
        return maximoDiario;
    }

    /** Reserva una consulta; devuelve false si ya se alcanzó el máximo de hoy. */
    public boolean intentar(Long idCliente) {
        LocalDate hoy = LocalDate.now(reloj);
        boolean[] permitido = {false};
        usos.compute(idCliente, (id, anterior) -> {
            int usadas = anterior == null || !anterior.fecha().equals(hoy) ? 0 : anterior.cantidad();
            if (usadas >= maximoDiario) return new Uso(hoy, usadas);
            permitido[0] = true;
            return new Uso(hoy, usadas + 1);
        });
        return permitido[0];
    }

    /** Devuelve una consulta que no llegó a responderse (por ejemplo, si Gemini falló). */
    public void devolver(Long idCliente) {
        LocalDate hoy = LocalDate.now(reloj);
        usos.computeIfPresent(idCliente, (id, uso) -> uso.fecha().equals(hoy) && uso.cantidad() > 0
                ? new Uso(hoy, uso.cantidad() - 1) : uso);
    }

    public int restantes(Long idCliente) {
        Uso uso = usos.get(idCliente);
        int usadas = uso == null || !uso.fecha().equals(LocalDate.now(reloj)) ? 0 : uso.cantidad();
        return Math.max(0, maximoDiario - usadas);
    }

    private record Uso(LocalDate fecha, int cantidad) { }
}
