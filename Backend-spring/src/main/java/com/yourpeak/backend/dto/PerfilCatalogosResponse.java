package com.yourpeak.backend.dto;

import java.util.List;

public record PerfilCatalogosResponse(
        List<OpcionPerfilResponse> objetivos,
        List<OpcionPerfilResponse> nivelesExperiencia,
        List<OpcionPerfilResponse> nivelesActividad
) { }
