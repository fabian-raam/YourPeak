package com.yourpeak.backend.dto;

import java.util.List;

/**
 * Propuesta de rutina que todavía no se guarda: el cliente la revisa y la guarda
 * con el endpoint habitual de rutina.
 *
 * @param fuente  GEMINI si la propuso la IA o REGLAS si se usó el generador de respaldo
 * @param mensaje explicación para mostrar al cliente
 */
public record RutinaGeneradaResponse(String fuente, String mensaje, List<DiaRutinaResponse> dias) { }
