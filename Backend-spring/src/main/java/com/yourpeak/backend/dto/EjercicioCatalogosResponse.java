package com.yourpeak.backend.dto;

import java.util.List;

public record EjercicioCatalogosResponse(
        List<OpcionCatalogoResponse> gruposMusculares,
        List<OpcionCatalogoResponse> nivelesExperiencia,
        List<OpcionCatalogoResponse> tiposMedicion
) { }
