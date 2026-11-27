package com.yourpeak.backend.dto;

/** Respuesta del asistente; enTema=false indica que la pregunta no era de alimentación o ejercicio. */
public record AsistenteResponse(String respuesta, boolean enTema, int consultasRestantesHoy, int limiteDiario) { }
