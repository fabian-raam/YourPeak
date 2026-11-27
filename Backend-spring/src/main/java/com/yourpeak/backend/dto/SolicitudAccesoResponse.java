package com.yourpeak.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Estado de la cuenta del cliente y el historial de sus solicitudes de acceso. */
public record SolicitudAccesoResponse(
        String estado,
        boolean puedeSolicitar,
        int solicitudesRestantesHoy,
        int limiteDiario,
        List<Item> historial
) {
    public record Item(
            int numero,
            String estado,
            String mensajeCliente,
            String motivoRechazo,
            LocalDateTime fechaSolicitud,
            LocalDateTime fechaResolucion
    ) { }
}
