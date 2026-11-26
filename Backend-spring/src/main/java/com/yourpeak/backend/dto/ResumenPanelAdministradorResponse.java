package com.yourpeak.backend.dto;

/** Contadores actuales que alimentan la pantalla de inicio administrativo. */
public record ResumenPanelAdministradorResponse(
        long clientesActivos,
        long solicitudesPendientes,
        long clientesInactivos,
        long planesMembresiaActivos,
        long ejerciciosActivos,
        long alimentosCatalogo
) { }
