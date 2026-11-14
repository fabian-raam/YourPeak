package com.yourpeak.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PerfilInicialResponse(
        boolean completado,
        Integer idTipoObjetivo,
        String objetivo,
        Integer idNivelExperiencia,
        String nivelExperiencia,
        Integer idNivelActividad,
        String nivelActividad,
        BigDecimal pesoKg,
        BigDecimal tallaCm,
        LocalDateTime fechaRegistroFisico,
        PerfilCatalogosResponse catalogos
) { }
